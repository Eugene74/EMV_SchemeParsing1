package emvparsing;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

  class SupportClass {
    static void resultMethod(String emv_schemeString, String paramScheme, String timestampStart) {
        boolean isSuitability = checkParamScheme(emv_schemeString, paramScheme);
        if (isSuitability && !emv_schemeString.equals("empty")) {
            System.out.println("Входящие параметры: \"" + timestampStart + "\", \"M/Chip " + emv_schemeString + "\"- Тип приложения соответствует ожидаемому");
        } else {
            if (!emv_schemeString.equals("empty")) {
                System.out.println("Входящие параметры: \"" + timestampStart + "\", \"" + paramScheme + "\"- Тип приложения не соответствует ожидаемому");
            } else {
                System.out.println("Входящие параметры: \"" + timestampStart + " - нет тип параметра приложения");
            }
        }
    }

    static String getTimestamp_start(Integer i, Map map, String logString) {
        return searchNearestObject(i, map, logString);
    }

    static String searchNearestObject(Integer i, Map map, String logStr) {
        List<Integer> list = new ArrayList<>();
        int posStart = -1; // это начало икомого
        int posEnd = -1; // конец искомого
        Set<Map.Entry<Integer, String>> set2 = map.entrySet();
        for (Map.Entry<Integer, String> entry : set2) {
            int k = entry.getKey() - i;
            list.add(k);
        }
        int tmp = 0;
        int[] ints = new int[list.size()];
        for (int q : list) {
            ints[tmp] = q;
            tmp++;
        }
        Arrays.sort(ints);
        for (int j = 0; j < ints.length; j++) {
            if (ints[j] > 0) { //потому что отрицательные числа в массиве бывают а "ближайший" начинается с 0 (отрицательные потому что есть выше по списку транзакции от искомого элемента)
                posStart = ints[j] + i;
                break;
            }
        }
        if (posStart != -1) posEnd = ((String) map.get(posStart)).length() + posStart;
        return logStr.substring(posStart, posEnd);
    }

    static List<Integer> getList_KeyIntMap(Map map) {
        List<Integer> list = new ArrayList<>();
        Set<Map.Entry<Integer, String>> set1 = map.entrySet();
        for (Map.Entry<Integer, String> entry : set1) {
            int st = entry.getKey();
            list.add(st);
        }
        return list;
    }

    static List<String> getList_KeyStringMap(Map map) {
        List<String> list1 = new ArrayList<>();
        Set<Map.Entry<String, String>> set1 = map.entrySet();
        for (Map.Entry<String, String> entry : set1) {
            list1.add(entry.getKey());
        }
        return list1;
    }

    static void printTreeMap(Map treeMap_begin) {
        Set<Map.Entry<Integer, Integer>> set1 = treeMap_begin.entrySet();
        for (Map.Entry<Integer, Integer> entry : set1) {
            System.out.println(/*entry.getKey()+"-- "+*/entry.getValue());
        }
    }

    static Map changeToTreeMap(Map mapStartEndBegin, String logString) {
        Map treemap = new TreeMap();
        Set<Map.Entry<Integer, Integer>> set1 = mapStartEndBegin.entrySet();
        for (Map.Entry<Integer, Integer> entry : set1) {
            treemap.put(entry.getKey(), logString.substring(entry.getKey(), entry.getValue()));
        }
        return treemap;
    }

    static boolean checkTransWithoutScheme(Integer start, Integer end, String logString) {
        int s = -1, e = -1;
        boolean b = false;
        s = start;
        e = end;

        if (s > 0 & e > 0) {
            int pos = logString.substring(s, e).indexOf("EMV Scheme Matched:");
            if (pos > 0) b = true; // значит в этом промежутке есть искомый параметр приложения
        }
        return b;
    }

    private static boolean checkParamScheme(String scheme_param, String paramScheme) {
        return scheme_param.equals(paramScheme);
    }

    static String extractSchemeParam(String emv_schemeString) {
        int length = emv_schemeString.length();
        int index_schemeStart = ("EMV Scheme Matched: [M/Chip ").length();
        return emv_schemeString.substring(index_schemeStart, length - 1);
    }

    static Map getPositionStartEnd(String logStr, String element) {

        Map mapPositionStartEnd = new HashMap();
        Pattern pattern = Pattern.compile(element);
        Matcher matcher = pattern.matcher(logStr);
        while (matcher.find()) {
            mapPositionStartEnd.put(matcher.start(), matcher.end());
        }
        return mapPositionStartEnd;
    }

    static String performRequest(String urlStr) throws IOException {

        byte[] buf;
        try (RandomAccessFile f = new RandomAccessFile(urlStr, "r")) {
            try {
                buf = new byte[(int) f.length()];
                f.read(buf, 0, buf.length);
            } finally {
                f.close();
            }
        }
        return new String(buf);
    }
}
