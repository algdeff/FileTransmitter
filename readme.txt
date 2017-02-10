������ ������� � ������ Transmitter
(������ ������������ �� JDK 8u121)

���� ��� maven � JRE, 1-4 ��� ���:
1. ��������� maven: http://maven.apache.org/download.cgi
2. ������������� ����� � maven �� ����
3. ��������� � ���������� %PATH% ���� � maven/bin, �������� c:\apache-maven-3.3.9\bin\
4. ��������� � ������������� JDK8 ��� ����� �������: http://www.oracle.com/technetwork/java/javase/downloads/2133151
5. ��������� � ���������� %PATH% � %JAVA_HOME% ���� � JDK
5. ��������� � ���������� Transmitter
6. ��� ������ ������� � ������� maven �����: mvn compile
7. ��������� Transmitter: mvn exec:java -Dexec.mainClass="Transmitter.ServerStarter"
8. ����� �� ���������: CTRL+C

����� ���-�� ������� ������ � ���� ����������� .jar ����, �� ����� �������� � �������������:
1. ������� ����������� .jar: mvn package
2. ���� �������� � target/Transmitter-......-jar-with-dependencies.jar (����. ��������� ������� 'maven-assembly-plugin' � pom.xml)
3. ���� ����� ����������� � ����� ����� (������� �� "progFolder")
4. � ����� � ������ (progFolder) ������ ��������� ���� �������� filetransmitter.conf.xml
6. ������ .jar �� JVM: java -jar Transmitter-..(������)..-jar-with-dependencies.jar

===============================================================================================================
���������������� ����: xmlwfiletransmitterorker.conf.xml


    <!--URL ���������� �������-->
    <remote_server_url>localhost</remote_server_url>

    <!--PORT ���������� �������-->
    <remote_server_port>5252</remote_server_port>

    <!--���� ����� ��� ����������� ������-->
    <received_path>_received</received_path>

    <!--���� ����� ��� �������������� �������� ������ �� ��������� ������-->
    <outcoming_path>_outcome</outcoming_path>

    <!--���� ����� ��� ������������ ������-->
    <sent_path>_sent</sent_path>

    <!--���� � ��� LOG �����-->
    <log_file_path_name>result.log</log_file_path_name>

    <!--����. ���������� ������������ �������-->
    <thread_pool_size>50</thread_pool_size>

    <!--����� ������������� ������������ ������-->
    <outcoming_file_type_glob>*.{zip,txt,xml,exe,doc}</outcoming_file_type_glob>
