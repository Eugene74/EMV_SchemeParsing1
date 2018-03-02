package emvparsing;

import java.io.IOException;

import java.util.*;
import static emvparsing.SupportClass.*;

public class Main {
    public static void main(String[] args) throws IOException {

        int map_size;
        String transactionNumber;
        String emv_schemeAll= "EMV Scheme Matched:\\s\\[M/Chip\\s.*]";
        String timestampAll = "\\[\\d\\d\\d\\d-\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d]";

        String logFilePath;
        String log1="A.log";
        String log2="B.log";
        System.out.println("Выбирите файл логов: A.log - 1; B.log - 2");
        Scanner scanner = new Scanner(System.in);
        String logFile = scanner.nextLine();
        if (logFile.equals("1")){logFilePath = log1;}else logFilePath = log2;
        String request1 ="C:\\Users\\1\\Desktop\\zip\\"+logFilePath;
        String logString = performRequest(request1);


// все начала таймстампов   триМАПА  - это координата- начало(13) -и  строка( полная - [2018-02-19 16:35:59.229] )
        Map mapStartEnd_TimestampAll =getPositionStartEnd(logString,  timestampAll);
        Map treeMap_timestamps= changeToTreeMap(mapStartEnd_TimestampAll, logString);
      //   printTreeMap(treeMap_timestamps);
// все начала транзакций   триМАПА - это  координата- начало строки-и  строка полная( полная - Transaction 1 begin: )
        Map mapStartEndBeginAll =getPositionStartEnd(logString,  "Transaction.*begin:");
        Map treeMap_begin= changeToTreeMap(mapStartEndBeginAll, logString);
      //  printTreeMap(treeMap_begin);
// все конецы транзакций  триМАПА- это  координата- начало строки-и  строка полная( полная - Transaction 1 end ... some text: )
        Map mapStartEnd_EndsAll =getPositionStartEnd(logString,  "Transaction.*\\s.*\\s.*(end:|end.*[a-z]:)");
        Map treeMap_ends= changeToTreeMap(mapStartEnd_EndsAll, logString);
      //printTreeMap(treeMap_ends);
// все М/ШЕМА   триМАПА координата начало - строка полная ( EMV Scheme Matched: [M/Chip 2.1 or 2.2] )
        Map mapStartEnd_Emv_schemeAll =getPositionStartEnd(logString,  emv_schemeAll);
        Map treeMap_schemes= changeToTreeMap(mapStartEnd_Emv_schemeAll, logString);
      // printTreeMap(treeMap_schemes);
// все Исключения  начало    триМАПА координата начало - строка полная (INFO Some log data, transact 3 start)
        Map mapStart_Exceptions_startAll =getPositionStartEnd(logString,  "INFO Some log data, transact .* start");
        Map treeMap_exception= changeToTreeMap(mapStart_Exceptions_startAll, logString);
       //  printTreeMap(treeMap_exception);
// все Исключения  конц   триМАПА координата начало - строка полная (Sending reply to)
        Map mapStart_Exceptions_reply =getPositionStartEnd(logString,  "Sending reply to");
        Map treeMap_replay= changeToTreeMap(mapStart_Exceptions_reply, logString);
      //  printTreeMap(treeMap_replay);


//объединённые начала транзакций и исключений
        Map unted_map_begin = new TreeMap();
        unted_map_begin.putAll(treeMap_begin);
        unted_map_begin.putAll(treeMap_exception);
      // System.out.println("------------");
     //  printTreeMap(unted_map_begin);
//объединённые конец транзакций и исключений
        Map unted_map_ends = new TreeMap();
        unted_map_ends.putAll(treeMap_ends);
        unted_map_ends.putAll(treeMap_replay);
    //    System.out.println("------------");
    //    printTreeMap(unted_map_ends);
        Map finalSpecialMap = new TreeMap(); // мапа -  итоговая


// для того, чтобы по очереди каждую транзакцию и её конец брать и что-то делать ...
        List<Integer> starts = getList_KeyIntMap(unted_map_begin);//координаты   начала begin:_
        List<Integer> ends = getList_KeyIntMap(unted_map_ends); //координаты   начала end:_
        String timestampString;
        String emv_schemeString;
// вдруг какой-то конец транзакции не записался, тогда эти массивы не будут равны....
        if (starts.size() == ends.size()) {

// сборка специальной мапы где ключ - это "таймстамп",  а значение это параметр параметр Scheme Matched: ---тут и исключения тоже со свими параметрами
            for (int i = 0; i < starts.size(); i++) {
                int startSearch= starts.get(i)+((String) unted_map_begin.get(starts.get(i))).length(); // начало промежутка   конец "Transaction 1 begin:_"
                int endSearch=ends.get(i);  //   конец промежутка  -  начало "Transaction 1 end ... some text:"
                boolean c = checkTransWithoutScheme(startSearch, endSearch, logString); // работаю с каждым по очереди из списка
                    if (c) {
                    timestampString=getTimestamp_start(starts.get(i),treeMap_timestamps, logString);
                    emv_schemeString= searchNearestObject(starts.get(i),treeMap_schemes, logString);
                    //System.out.println(timestampString+" "+emv_schemeString);
                    String scheme_param = extractSchemeParam(emv_schemeString);
                    finalSpecialMap.put(timestampString,scheme_param);
                }else {
                     timestampString=getTimestamp_start(starts.get(i),treeMap_timestamps, logString);
                     System.out.println("у транзакции " + (i+1) + " нет типа параметра приложения");
                     finalSpecialMap.put(timestampString,"empty");//тогда в мапу тр -- эмпти
                }
            }
           // printTreeMap(finalSpecialMap);
// а теперь запрос по конкретной транзакции
            map_size = unted_map_begin.size();
            printTreeMap(treeMap_begin);
// без попавших в общюю мапу исключений - treeMap_exception.size())
            System.out.println("В данном лог_файле отражены "+(map_size-(treeMap_exception.size()))+" транзакции,какой из них вы хотите знать соответствие EMV_Scheme ?");

            System.out.println(" Ввведите,пожалуйста, необходимый номер...");
            transactionNumber=scanner.nextLine();
            int transactNum_parse = Integer.parseInt(transactionNumber)-1;
            System.out.println("Введите параметр для проверки соответствия EMV_Scheme: (Например: (2.1 or 2.2) или (2.05) или (Advance) ...или что угодно  - без скобок)");
            String paramScheme = scanner.nextLine(); // искомый параметр

            List<String> list = getList_KeyStringMap(finalSpecialMap); // список ключей

            timestampString = list.get(transactNum_parse); // беру ключ
            emv_schemeString = (String) finalSpecialMap.get(timestampString);// беру значение
            resultMethod(emv_schemeString,paramScheme, timestampString);
        }
    }
}
