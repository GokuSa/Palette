package beijing.hanhua.sketchpad.netty;


import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class IniReaderNoSection {
    public Properties properties = null;

    public IniReaderNoSection(String filename) {
        File file = new File(filename);
        try {
            properties = new Properties();
            properties.load(new FileInputStream(file));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getIniKey(String key) {
        if (properties.containsKey(key) == false) {
            return null;
        }

        return String.valueOf(properties.get(key));
    }
}

