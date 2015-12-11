package che.carleton.ottawa.bluestream;

/**
 * Created by CZL on 12/11/2015.
 */
public final class Settings {

    public static int QUALITY_LEVEL = 50;

    private Settings() {
        QUALITY_LEVEL = 50;
    }

    public void setQualityLevel(int i) {
        QUALITY_LEVEL = i;
    }

    public int getQualityLevel() {
        return QUALITY_LEVEL;
    }
}
