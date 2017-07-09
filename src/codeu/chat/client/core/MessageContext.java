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

package codeu.chat.client.core;

import java.util.Arrays;
import java.util.Iterator;

import codeu.chat.common.BasicView;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.Message;
<<<<<<< HEAD
import codeu.chat.common.User;
=======
import codeu.chat.util.Serializer;
import codeu.chat.util.Serializers;
>>>>>>> e8b7c466b90f32b9119079c9b925bdbe3d30292d
import codeu.chat.util.Uuid;

public final class MessageContext {
  
  public final Message message;
<<<<<<< HEAD
  private final ConversationHeader conversation;
  private final User user;
  private final BasicView view;
=======
  public final BasicView view;
>>>>>>> e8b7c466b90f32b9119079c9b925bdbe3d30292d

  public MessageContext(Message message, ConversationHeader conversation, User user, BasicView view) {
    this.message = message;
    this.conversation = conversation;
    this.user = user;
    this.view = view;
  }

  public MessageContext next() {
    return message.next == null ? null : getMessage(message.next);
  }

  public MessageContext previous() {
    return message.previous == null ? null : getMessage(message.previous);
  }

  private MessageContext getMessage(Uuid id) {
    final Iterator<Message> messages = view.getMessages(conversation, user, Arrays.asList(id)).iterator();
    return messages.hasNext() ? new MessageContext(messages.next(), conversation, user, view) : null;
  }
}
