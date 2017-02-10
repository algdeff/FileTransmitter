сборка проекта и запуск Transmitter
(проект тестировался на JDK 8u121)

Если нет maven и JRE, 1-4 для Вас:
1. скачиваем maven: http://maven.apache.org/download.cgi
2. распаковываем архив с maven на диск
3. добавляем в переменные %PATH% путь к maven/bin, например c:\apache-maven-3.3.9\bin\
4. скачиваем и устанавливаем JDK8 для Вашей системы: http://www.oracle.com/technetwork/java/javase/downloads/2133151
5. добавляем в переменные %PATH% и %JAVA_HOME% путь к JDK
5. переходим в директорию Transmitter
6. для сборки проекта с помошью maven пишем: mvn compile
7. запускаем Transmitter: mvn exec:java -Dexec.mainClass="Transmitter.ServerStarter"
8. выход из программы: CTRL+C

Можно так-же собрать проект в один исполняемый .jar файл, со всеми классами и зависимостями:
1. Создаем исполняемый .jar: mvn package
2. Файл появится в target/Transmitter-......-jar-with-dependencies.jar (прим. настройки плагина 'maven-assembly-plugin' в pom.xml)
3. Файл можно переместить в любую папку (назовем ее "progFolder")
4. В папке с файлом (progFolder) должен находится файл настроек transmitter.conf.xml
6. Запуск .jar на JVM: java -jar Transmitter-..(версия)..-jar-with-dependencies.jar

===============================================================================================================
Конфигурационный файл: transmitterorker.conf.xml


    <!--URL удаленного сервера-->
    <remote_server_url>localhost</remote_server_url>

    <!--PORT удаленного сервера-->
    <remote_server_port>5252</remote_server_port>

    <!--путь папки для загруженных файлов-->
    <received_path>_received</received_path>

    <!--путь папки для автоматической отправки файлов на удаленный сервер-->
    <outcoming_path>_outcome</outcoming_path>

    <!--путь папки для отправленных файлов-->
    <sent_path>_sent</sent_path>

    <!--путь и имя LOG файла-->
    <log_file_path_name>result.log</log_file_path_name>

    <!--макс. количество параллельных потоков-->
    <thread_pool_size>50</thread_pool_size>

    <!--маски автоматически отправляемых файлов-->
    <outcoming_file_type_glob>*.{zip,txt,xml,exe,doc}</outcoming_file_type_glob>
