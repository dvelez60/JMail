// gmail client using CLI

import java.io.*;
import java.util.*;
import java.net.InetAddress;

import javax.mail.*;
import javax.mail.internet.*;
import com.sun.mail.smtp.*;

import org.jsoup.*;
import org.jsoup.nodes.Document;

class Mail{

 // mail server
 private InternetAddress myEmail;
 private InternetAddress recEmail;
 private Properties properties;
 private Session session;
 private MimeMessage message;
 private SMTPTransport t;
 // terminal colors
 public static final String RESET = "\u001B[0m";
 public static final String GREEN = "\u001B[32m";
 public static final String RED = "\u001B[31m";
 public static final String CYAN = "\u001B[36m";
 public static final String YELLOW = "\u001B[33m";
 public static final String PURPLE = "\u001B[35m";
 // program
 private boolean program = true;
 private BufferedReader cin;
 private String option;
 private String[] cmdArgs = new String[4];
 private int numArgs = 0;
 private Message[]msgs;
 private String myEmailS, myPassword;
 private String saveFile = "login.inf";

 void go(){

  setInputMode("raw");

  System.out.println(PURPLE + "\ngmail" + RESET);

  cin = new BufferedReader
  (new InputStreamReader(System.in));

  try{

   setupServer();
   autoLogin();

   while(program){

    System.out.println(CYAN + "\noptions[login][send][inbox][open][help][exit]" + RESET);
    System.out.println(GREEN + "logged in as: " + myEmail + RESET);

    System.out.flush();
    option = cin.readLine();
    parseOption(option);
    option = cmdArgs[0];

    switch(option){

     case"login":
      login();
     break;

     case"send":
      sendMail();
     break;

     case"exit":
      exit();
     break;

     case"help":
     break;

     case"inbox":
      readMail();
     break;

     case"open":
      openMessage();
     break;

     default:
      error("cmd");
     break;

    }

   }

  }catch(AuthenticationFailedException aue){

   error("loggedIn");

  }catch(Exception ex){

   ex.printStackTrace();

  }

 }

 void usage(String type){

  switch(type){

   case"login":
    System.out.println(YELLOW + "Usage: login [USERNAME] [PASSWORD]" + RESET);
   break;

   case"inbox":
    System.out.println(YELLOW + "Usage: inbox [NUMBER OF MESSAGES]" + RESET);
   break;

   case"open":
    System.out.println(YELLOW + "Usage: open [MESSAGE INDEX]" + RESET);
   break;

  }

 }

 void parseOption(String str){

  clearArgs();
  cmdArgs = str.split("\\s+");

 }

 void setupServer()throws Exception{

  properties = System.getProperties();
  properties.put("mail.smtps.host", "smtp.gmail.com");
  properties.put("mail.smtps.auth", "true");

  session = Session.getInstance(properties, null);

  message = new MimeMessage(session);

  t = (SMTPTransport)session.getTransport("smtps");

 }

 void login()throws Exception{

  try{

   myEmail = new InternetAddress(cmdArgs[1]);

   message.setFrom(myEmail);

   System.out.print(YELLOW + "logging in..." + RESET);
   t.connect("smtp.gmail.com", cmdArgs[1], cmdArgs[2]);
   System.out.println(YELLOW + "Login succesful" + RESET);
   myEmailS = cmdArgs[1];
   myPassword = cmdArgs[2];
   saveLoginInfo();

  }catch(ArrayIndexOutOfBoundsException ex){

   error("args");
   usage("login");

  }catch(AuthenticationFailedException afex){

   error("login");

  }catch(com.sun.mail.util.MailConnectException mcex){

   error("connect");
   myEmail = null;

  }

 }

 void saveLoginInfo()throws Exception{

  FileWriter writer = new FileWriter(saveFile);
  writer.write(myEmailS + '\n' + myPassword);
  writer.close();

 }

 void loadLoginInfo()throws Exception{

  BufferedReader br = new BufferedReader
  (new FileReader(saveFile));

  String line = "";
  int ind = 1;
  while((line = br.readLine()) != null){

   cmdArgs[ind] = line;
   ind++;

  }
  myEmailS = cmdArgs[1];
  myPassword = cmdArgs[2];

 }

 void autoLogin()throws Exception{

  try{

   loadLoginInfo();
   login();

  }catch(FileNotFoundException fe){

   // no login info saved

  }

 }

 void addRecipients()throws Exception{

  for(int i = 0; i < cmdArgs.length; i++){

   if(cmdArgs[i] != null){

    recEmail = new InternetAddress(cmdArgs[i]);
    message.addRecipient(Message.RecipientType.TO, recEmail);

   }

  }

 }

 void openMessage(){

  try{

   parseMessage(msgs[Integer.parseInt(cmdArgs[1])]);

  }catch(ArrayIndexOutOfBoundsException aex){

   error("inbox");
   //e.printStackTrace();

  }catch(Exception e){

   usage("open");
   error("args");

  }

 }

 void readMail()throws Exception{

   try{

    properties.setProperty("mail.store.protocol", "imaps");

    Store store = session.getStore("imaps");
    store.connect
    ("imap.googlemail.com", myEmailS, myPassword);

    Folder eFolder = store.getFolder("INBOX");
    eFolder.open(Folder.READ_ONLY);

    System.out.println
    (Arrays.toString(store.getDefaultFolder().list()));

    msgs = eFolder.getMessages();

    System.out.println(msgs.length + " total messages");
    int numMsgs = msgs.length;

    for(int i = numMsgs - Integer.parseInt(cmdArgs[1]); i < numMsgs; i++){

     Message msg = msgs[i];
     System.out.println("---------------------------------------");
     System.out.println("index: " + i);
     System.out.println(PURPLE + "Subject: " + msg.getSubject() + RESET);
     System.out.println(GREEN + "From: " + msg.getFrom()[0] + RESET);
     System.out.println("Date: " + msg.getReceivedDate());
     System.out.println("Type: " + msg.getContentType());

    }

   }catch(ArrayIndexOutOfBoundsException aex){

    error("args");
    usage("inbox");

   }catch(AuthenticationFailedException aue){

    error("loggedIn");

   }

 }

 void parseMessage(Part msg)throws Exception{

  if(msg.isMimeType("multipart/*")){

   Multipart mp = (Multipart)msg.getContent();
   int count = mp.getCount();
   for(int o = 0; o < count; o ++){

    parseMessage(mp.getBodyPart(o));

   }

  }else if (msg.isMimeType("TEXT/*")){

    Document doc = Jsoup.parse((String)msg.getContent());
    System.out.println("\n" + doc.body().text() + "\n");

  }

 }

 void sendMail()throws Exception{

  try{

   System.out.println(YELLOW + "**type ENDL + RETURN in message body to send**" + RESET);
   System.out.print(YELLOW + "CC: " + RESET);
   parseOption(cin.readLine());
   addRecipients();

   System.out.print(YELLOW + "Subject: " + RESET);
   message.setSubject(cin.readLine());

   String msgTest = "";
   String msg = "";
   while((msgTest = cin.readLine()) != null){

    if("ENDL".equals(msgTest))break;
    msg += msgTest + "\n";

   }

   System.out.println(YELLOW + "Sending..." + RESET);
   message.setText(msg);
   t.sendMessage(message, message.getAllRecipients());
   System.out.println(YELLOW + "Response: " + t.getLastServerResponse() + RESET);
   message = new MimeMessage(session);

  }catch(IllegalStateException isex){

   error("loggedIn");

  }catch(ArrayIndexOutOfBoundsException aex){

   error("args");

  }catch(NullPointerException npex){

   error("rec");

  }catch(AddressException adex){

   error("rec");

  }

 }

 void error(String type){

  switch(type){

  case"cmd":
   System.out.println(RED + "command not found" + RESET);
  break;

  case"args":
   System.out.println(RED + "invalid arguments" + RESET);
  break;

  case"login":
   System.out.println(RED + "username and password do not match" + RESET);
  break;

  case"loggedIn":
   System.out.println(RED + "not logged in" + RESET);
  break;

  case"rec":
   System.out.println(RED + "no recipients" + RESET);
  break;

  case"connect":
   System.out.println(RED + "could not connect" + RESET);
  break;

  case"inbox":
   System.out.println(RED + "run inbox command first" + RESET);
  break;

  }

 }

 void setInputMode(String mode){

  try{

   String[] cmd = {"bash", "rawMode.sh"};
   Process p = Runtime.getRuntime().exec(cmd);
   p.waitFor();

   BufferedReader br = new BufferedReader
   (new InputStreamReader(p.getInputStream()));
   String line = "";

   while((line = br.readLine()) != null){

    System.out.println(line);

   }

  }catch(Exception ex){

   ex.printStackTrace();

  }

 }

 void clearArgs(){

  for(int i = 0; i < cmdArgs.length; i++){
    cmdArgs[i] = null;
  }

 }

 void exit()throws Exception{

  t.close();
  setInputMode("sane");
  System.exit(0);

 }

 public static void main(String[] args){
  new Mail().go();
 }

}
