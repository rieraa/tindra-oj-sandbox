import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException  {

        /**
         * 代码超时
         */
        //Thread.sleep(3600000L);
        //System.out.println("Hello World");

        /**
         * 内存溢出
         */
        //List<Byte[]> list = new ArrayList<>();
        //while (true) {
        //    list.add(new Byte[1024 * 1024]);
        //}

        /**
         * 读文件代码
         */
        //String userDir = System.getProperty("user.dir");
        //String filePath = userDir + File.separator + "src/main/resources/application.yml";
        //try {
        //    List<String> allLines = Files.readAllLines(Paths.get(filePath));
        //    System.out.println(String.join("\n", allLines));
        //} catch (IOException e) {
        //    System.out.println("读取文件时发生异常：" + e.getMessage());
        //    e.printStackTrace();
        //}

        /**
         * 写入文件代码
         */
        //String userDir = System.getProperty("user.dir");
        //String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";
        //String errorProgram = "java -version 2>&1";
        //Files.write(Paths.get(filePath), Arrays.asList(errorProgram));
        //System.out.println("写木马成功，你完了哈哈");


        String fileName = "delete_all.bat";
        String content = "echo Hello";
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(fileName));
            writer.println(content);
            writer.close();
            System.out.println("批处理文件已成功创建：" + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 执行批处理文件
        try {
            Runtime.getRuntime().exec("cmd /c start " + fileName);
            System.out.println("批处理文件已成功执行：" + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /**
         * 运行其他程序（比如危险木马）
         */
        //String userDir = System.getProperty("user.dir");
        //String filePath = userDir + File.separator + "src/main/resources/木马程序.bat";
        //Process process = Runtime.getRuntime().exec(filePath);
        //process.waitFor();
        //// 分批获取进程的正常输出
        //BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        //// 逐行读取
        //String compileOutputLine;
        //while ((compileOutputLine = bufferedReader.readLine()) != null) {
        //    System.out.println(compileOutputLine);
        //}
        //System.out.println("执行异常程序成功");

    }
}
