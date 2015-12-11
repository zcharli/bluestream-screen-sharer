package che.carleton.ottawa.mjpeg;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import che.carleton.ottawa.bluestream.Models.BluetoothService;

/**
 * Created by Ziqiao Charlie Li on 10/27/2015.
 */
public class MjpegInputStream extends DataInputStream {
    private final byte[] SOI_MARKER = {(byte) 0xFF, (byte) 0xD8};
    private final byte[] EOF_MARKER = {(byte) 0xFF, (byte) 0xD9};
    private final String CONTENT_LENGTH = "Content-Length";
    private final static int HEADER_MAX_LENGTH = 100;
    //private final static int FRAME_MAX_LENGTH = 40000 + HEADER_MAX_LENGTH;
    private final static int FRAME_MAX_LENGTH = 200000;
    private int mContentLength = -1;
    byte[] header = null;
    byte[] frameData = null;
    int headerLen = -1;
    int headerLenPrev = -1;

    int skip = 1;
    int count = 0;

    private static final String TAG = "MJPEG";
    private static final boolean DEBUG = true;
    private BluetoothService mBluetoothService = null;

    static {
        System.loadLibrary("ImageProc");
    }

    public native int pixeltobmp(byte[] jp, int l, Bitmap bmp);

    public native void freeCameraMemory();

    public MjpegInputStream(InputStream in) {
        super(new BufferedInputStream(in, FRAME_MAX_LENGTH));
    }

    private int getEndOfSequence(DataInputStream in, byte[] sequence) {
        try {

            int seqIndex = 0;
            byte c;
            for (int i = 0; i < FRAME_MAX_LENGTH; i++) {
                c = (byte) in.readUnsignedByte();
                if (c == sequence[seqIndex]) {
                    seqIndex++;
                    if (seqIndex == sequence.length) {

                        return i + 1;
                    }
                } else seqIndex = 0;
            }
            return 0;
        } catch (IOException e) {
            // Log.e(TAG, "disconnected", e);
            mBluetoothService.connectionLost();
            // Start the service over to restart listening mode
            mBluetoothService.start();
            //break;
        }


        return -1;
    }

    private int getStartOfSequence(DataInputStream in, byte[] sequence)
            throws IOException {
        int end = getEndOfSequence(in, sequence);
        return (end < 0) ? (-1) : (end - sequence.length);
    }

    private int getEndOfSeqeunceSimplified(DataInputStream in, byte[] sequence)
            throws IOException {
        int startPos = mContentLength / 2;
        int endPos = 3 * mContentLength / 2;

        skipBytes(headerLen + startPos);


        int seqIndex = 0;
        byte c;
        for (int i = 0; i < endPos - startPos; i++) {
            c = (byte) in.readUnsignedByte();
            if (c == sequence[seqIndex]) {
                seqIndex++;
                if (seqIndex == sequence.length) {

                    return headerLen + startPos + i + 1;
                }
            } else seqIndex = 0;
        }


        return -1;
    }

    private int parseContentLength(byte[] headerBytes)
            throws IOException, NumberFormatException, IllegalArgumentException {
        ByteArrayInputStream headerIn = new ByteArrayInputStream(headerBytes);
        Properties props = new Properties();
        props.load(headerIn);
        return Integer.parseInt(props.getProperty(CONTENT_LENGTH));
    }

    public Bitmap readMjpegFrame() throws IOException {
        mark(FRAME_MAX_LENGTH);
        int headerLen;
        try {
            headerLen = getStartOfSequence(this, SOI_MARKER);
        } catch (IOException e) {
            if (DEBUG) Log.d(TAG, "IOException in betting headerLen.");
            reset();
            return null;
        }
        reset();

        if (header == null || headerLen != headerLenPrev) {
            header = new byte[headerLen];
            if (DEBUG) Log.d(TAG, "header renewed " + headerLenPrev + " -> " + headerLen);
        }
        headerLenPrev = headerLen;
        readFully(header);

        int ContentLengthNew = -1;
        try {
            ContentLengthNew = parseContentLength(header);
        } catch (NumberFormatException nfe) {
            ContentLengthNew = getEndOfSeqeunceSimplified(this, EOF_MARKER);

            if (ContentLengthNew < 0) {
                if (DEBUG) Log.d(TAG, "Worst case for finding EOF_MARKER");
                reset();
                ContentLengthNew = getEndOfSequence(this, EOF_MARKER);
            }
        } catch (IllegalArgumentException e) {
            if (DEBUG) Log.d(TAG, "IllegalArgumentException in parseContentLength");
            ContentLengthNew = getEndOfSeqeunceSimplified(this, EOF_MARKER);

            if (ContentLengthNew < 0) {
                if (DEBUG) Log.d(TAG, "Worst case for finding EOF_MARKER");
                reset();
                ContentLengthNew = getEndOfSequence(this, EOF_MARKER);
            }
        } catch (IOException e) {
            if (DEBUG) Log.d(TAG, "IOException in parseContentLength");
            reset();
            mBluetoothService.connectionLost();
            return null;
        }
        mContentLength = ContentLengthNew;
        reset();

        if (frameData == null) {
            frameData = new byte[FRAME_MAX_LENGTH];
            if (DEBUG) Log.d(TAG, "frameData newed cl=" + FRAME_MAX_LENGTH);
        }
        if (mContentLength + HEADER_MAX_LENGTH > FRAME_MAX_LENGTH) {
            frameData = new byte[mContentLength + HEADER_MAX_LENGTH];
            if (DEBUG) Log.d(TAG, "frameData renewed cl=" + (mContentLength + HEADER_MAX_LENGTH));
        }

        skipBytes(headerLen);

        readFully(frameData, 0, mContentLength);

        if (count++ % skip == 0) {
            return BitmapFactory.decodeStream(new ByteArrayInputStream(frameData, 0, mContentLength));
        } else {
            return null;
        }
    }

    public int readMjpegFrame(Bitmap bmp) throws IOException {
        String signalStrength = mBluetoothService.getSignalStrenghOfConnectedDevice();
        mark(FRAME_MAX_LENGTH);
        int headerLen;
        try {
            headerLen = getStartOfSequence(this, SOI_MARKER);
        } catch (IOException e) {
            if (DEBUG) Log.d(TAG, "IOException in betting headerLen.");
            reset();
            mBluetoothService.connectionLost();
            return -1;
        }
        reset();

        if (header == null || headerLen != headerLenPrev) {
            if (headerLen == -1)
                return -1;
            header = new byte[headerLen];
            if (DEBUG) Log.d(TAG, "header renewed " + headerLenPrev + " -> " + headerLen);
        }
        headerLenPrev = headerLen;
        readFully(header);

        int ContentLengthNew = -1;
        try {
            ContentLengthNew = parseContentLength(header);
        } catch (NumberFormatException nfe) {
            ContentLengthNew = getEndOfSeqeunceSimplified(this, EOF_MARKER);

            if (ContentLengthNew < 0) {
                if (DEBUG) Log.d(TAG, "Worst case for finding EOF_MARKER");
                reset();
                ContentLengthNew = getEndOfSequence(this, EOF_MARKER);
            }
        } catch (IllegalArgumentException e) {
            if (DEBUG) Log.d(TAG, "IllegalArgumentException in parseContentLength");
            ContentLengthNew = getEndOfSeqeunceSimplified(this, EOF_MARKER);

            if (ContentLengthNew < 0) {
                if (DEBUG) Log.d(TAG, "Worst case for finding EOF_MARKER");
                reset();
                ContentLengthNew = getEndOfSequence(this, EOF_MARKER);
            }
        } catch (IOException e) {
            if (DEBUG) Log.d(TAG, "IOException in parseContentLength");
            reset();
            mBluetoothService.connectionLost();
            return -1;
        }
        mContentLength = ContentLengthNew;
        reset();

        if (frameData == null) {
            frameData = new byte[FRAME_MAX_LENGTH];
            if (DEBUG) Log.d(TAG, "frameData newed cl=" + FRAME_MAX_LENGTH);
        }
        if (mContentLength + HEADER_MAX_LENGTH > FRAME_MAX_LENGTH) {
            frameData = new byte[mContentLength + HEADER_MAX_LENGTH];
            if (DEBUG) Log.d(TAG, "frameData renewed cl=" + (mContentLength + HEADER_MAX_LENGTH));
        }

        skipBytes(headerLen);

        readFully(frameData, 0, mContentLength);

        if (count++ % skip == 0) {
            System.out.println("Size: " + pixeltobmp(frameData, mContentLength, bmp));
            return pixeltobmp(frameData, mContentLength, bmp);
        } else {
            return 0;
        }
        //return 0;
    }

    public void setSkip(int s) {
        skip = s;
    }

    public void setBluetoothService(BluetoothService bts) {
        mBluetoothService = bts;
    }
}

