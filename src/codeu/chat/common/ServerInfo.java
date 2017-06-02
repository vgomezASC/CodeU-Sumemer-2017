package codeu.chat.common;

import java.io.IOException;

import codeu.chat.util.Uuid;
public final class ServerInfo {
  private final static String SERVER_VERSION = "1.0.0";

  public final Uuid version;
  public ServerInfo(){
      Uuid temp = null;
      try {
        temp = Uuid.parse(SERVER_VERSION);
      } catch (IOException e) {
          System.out.println("Fatal Error: Cannot parse the server infomation. Line 13, ServerInfo.java");
      }
      finally
      {
        this.version = temp;
      }
  }

  public ServerInfo(Uuid version) {
    this.version = version;
  }
}