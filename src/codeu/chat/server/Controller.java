// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package codeu.chat.server;

import java.io.File;
import java.util.LinkedHashSet;

import codeu.chat.common.BasicController;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ConversationHeader.ConversationUuid;
import codeu.chat.common.ConversationPayload;
import codeu.chat.common.InterestSet;
import codeu.chat.common.Message;
import codeu.chat.common.RandomUuidGenerator;
import codeu.chat.common.RawController;
import codeu.chat.common.User;
import codeu.chat.server.LocalFile;
import codeu.chat.util.Logger;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;

public final class Controller implements RawController, BasicController {
  public static final byte USER_TYPE_CREATOR = 0b111;
  public static final byte USER_TYPE_OWNER = 0b011;
  public static final byte USER_TYPE_MEMBER = 0b001;
  public static final byte USER_TYPE_BANNED = 0b000;
	 
  private final static Logger.Log LOG = Logger.newLog(Controller.class);

  private final Model model;
  private final Uuid.Generator uuidGenerator;

  private final LocalFile localFile;

  public Controller(Uuid serverId, Model model) {
    this.model = model;
    this.uuidGenerator = new RandomUuidGenerator(serverId, System.currentTimeMillis());
    this.localFile = new LocalFile(new File("."));
  }
  //New constructor, which can get the local file information.
  public Controller(Uuid serverId, Model model,LocalFile localFile) {
    this.model = model;
    this.uuidGenerator = new RandomUuidGenerator(serverId, System.currentTimeMillis());
    
    this.localFile = localFile;//The path is assigned by server.

    LinkedHashSet<User> localUsers = localFile.getUsers();
    LinkedHashSet<ConversationHeader> localConversations = localFile.getConversationHeaders();
    LinkedHashSet<Message> localMessages = localFile.getMessages();
    LinkedHashSet<AuthorityBuffer> localAuthority = localFile.getauthorityList();
    
    for(User item : localUsers)
    {
      newUser(item.id, item.name, item.creation);
    }

    for(ConversationHeader item : localConversations)
    {
      newConversation(item.id, item.title, item.owner, item.creation);
    }

    for(Message item : localMessages)
    {
      newMessage(item.id, item.author, item.conversation, item.content, item.creation);
    }
    for(AuthorityBuffer item : localAuthority)
    {
      model.initializeAuthority(item.conversation, item.user, item.authorityByte);
    }
  }

  @Override
  public Message newMessage(Uuid author, Uuid conversation, String body) {
    if(!model.isMember(conversation, author))
    {
      return null;
    }
    return newMessage(createId(), author, conversation, body, Time.now());
  }

  @Override
  public User newUser(String name) {
    return newUser(createId(), name, Time.now());
  }

  @Override
  public void authorityModificationRequest(ConversationUuid conversation, Uuid targetUser, Uuid fromUser, String parameterString){
    byte authorityByte = 0b000;
    if(parameterString.equals("o")){
      authorityByte = USER_TYPE_OWNER;
    } else if(parameterString.equals("m")){
      authorityByte = USER_TYPE_MEMBER;
    } else if(parameterString.equals("b")){
      authorityByte = USER_TYPE_BANNED;
    }
    model.changeAuthority(conversation, targetUser, authorityByte);
    localFile.addAuthority(conversation, targetUser, authorityByte);
  }
  
  @Override
  public ConversationHeader newConversation(String title, Uuid owner) {
	ConversationUuid chatId = new ConversationUuid(createId());
	return newConversation(chatId, title, owner, Time.now());
  }

  @Override
  public Message newMessage(Uuid id, Uuid author, Uuid conversation, String body, Time creationTime) {

    final User foundUser = model.userById().first(author);
    final ConversationPayload foundConversation = model.conversationPayloadById().first(conversation);

    Message message = null;

    if (foundUser != null && foundConversation != null && isIdFree(id)) {

      message = new Message(id, Uuid.NULL, Uuid.NULL, creationTime, author, body,conversation);
      model.add(message);
      localFile.addMessage(message);
      LOG.info("Message added: %s", message.id);

      // Find and update the previous "last" message so that it's "next" value
      // will point to the new message.

      if (Uuid.equals(foundConversation.lastMessage, Uuid.NULL)) {

        // The conversation has no messages in it, that's why the last message is NULL (the first
        // message should be NULL too. Since there is no last message, then it is not possible
        // to update the last message's "next" value.

      } else {
        final Message lastMessage = model.messageById().first(foundConversation.lastMessage);
        lastMessage.next = message.id;
      }

      // If the first message points to NULL it means that the conversation was empty and that
      // the first message should be set to the new message. Otherwise the message should
      // not change.

      foundConversation.firstMessage =
          Uuid.equals(foundConversation.firstMessage, Uuid.NULL) ?
          message.id :
          foundConversation.firstMessage;

      // Update the conversation to point to the new last message as it has changed.

      foundConversation.lastMessage = message.id;
    }

    return message;
  }

  @Override
  public User newUser(Uuid id, String name, Time creationTime) {

    User user = null;

    if (isIdFree(id)) {

      user = new User(id, name, creationTime);
      model.add(user);
      localFile.addUser(user);
      LOG.info(
          "newUser success (user.id=%s user.name=%s user.time=%s)",
          id,
          name,
          creationTime);

    } else {

      LOG.info(
          "newUser fail - id in use (user.id=%s user.name=%s user.time=%s)",
          id,
          name,
          creationTime);
    }

    return user;
  }
  @Override
  public ConversationHeader newConversation(ConversationUuid id, String title, Uuid owner, Time creationTime) {

    final User foundOwner = model.userById().first(owner);

    ConversationHeader conversation = null;
    if (foundOwner != null && isIdFree(id)) {
      conversation = new ConversationHeader(id, owner, creationTime, title); 
      System.out.println(conversation.id.toString());
      model.add(conversation);
      localFile.addConversationHeader(conversation);
      model.changeAuthority(conversation.id, owner, USER_TYPE_CREATOR);
      LOG.info("Conversation added: " + id);
    }

    return conversation;
  }
  
  @Override
  public InterestSet getInterestSet(Uuid id){
    return model.getInterestSet(id);
  }
  
  @Override
  public void updateInterests(Uuid id, InterestSet intSet){
    model.updateInterests(id, intSet);
  }
  
  private Uuid createId() {

    Uuid candidate;

    for (candidate = uuidGenerator.make();
         isIdInUse(candidate);
         candidate = uuidGenerator.make()) {

     // Assuming that "randomUuid" is actually well implemented, this
     // loop should never be needed, but just incase make sure that the
     // Uuid is not actually in use before returning it.

    }

    return candidate;
  }

  private boolean isIdInUse(Uuid id) {
    return model.messageById().first(id) != null ||
           model.conversationById().first(id) != null ||
           model.userById().first(id) != null;
  }

  private boolean isIdFree(Uuid id) { return !isIdInUse(id); }
}
