package ccq.core;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalizationTest {
    private static final Pattern ARGUMENT=Pattern.compile("%(?:(\\d+)\\$)?s");

    @Test void allCoreLanguagesHaveMatchingKeysAndFormatArguments()throws Exception {
        for(String namespace:List.of("ccq_core","ccq_testground","createaerophysicsgantry")) {
            var english=read(namespace,"en_us");
            for(String locale:List.of("zh_cn","ru_ru","zh_ls")) {
                var localized=read(namespace,locale);
                assertEquals(english.keySet(),localized.keySet(),namespace+" / "+locale+" translation coverage");
                for(var entry:english.entrySet()) {
                    String key=entry.getKey(),text=localized.get(key);
                    assertEquals(arguments(entry.getValue()),arguments(text),locale+" / "+key);
                    int count=arguments(text).stream().mapToInt(Integer::intValue).max().orElse(0);
                    Object[] values=new Object[count];Arrays.fill(values,"argument");
                    assertDoesNotThrow(()->String.format(Locale.ROOT,text,values),locale+" / "+key);
                }
            }
        }
    }

    private static Map<String,String> read(String namespace,String locale)throws Exception {
        var path=Path.of("src/main/resources/assets",namespace,"lang",locale+".json");
        String source=Files.readString(path,StandardCharsets.UTF_8);
        assertFalse(source.startsWith("\uFEFF"),path+" is UTF-8 without BOM");
        assertFalse(source.contains("\uFFFD"),path+" has no replacement characters");
        var result=new LinkedHashMap<String,String>();
        try(var reader=new JsonReader(new StringReader(source))) {
            reader.beginObject();
            while(reader.hasNext()) {
                String key=reader.nextName();assertEquals(JsonToken.STRING,reader.peek(),path+" / "+key);
                String value=reader.nextString();assertFalse(value.isBlank(),path+" / "+key);
                assertNull(result.put(key,value),path+" duplicate key: "+key);
            }
            reader.endObject();assertEquals(JsonToken.END_DOCUMENT,reader.peek());
        }
        return result;
    }

    private static Set<Integer> arguments(String text) {
        var result=new HashSet<Integer>();var matcher=ARGUMENT.matcher(text);int implicit=0;
        while(matcher.find())result.add(matcher.group(1)==null?++implicit:Integer.parseInt(matcher.group(1)));
        return result;
    }
}
