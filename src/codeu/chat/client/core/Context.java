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

import java.util.ArrayList;
import java.util.Collection;

import codeu.chat.common.InterestSet;
import codeu.chat.common.User;
import codeu.chat.common.ServerInfo;
import codeu.chat.util.Uuid;
import codeu.chat.util.connections.ConnectionSource;

public final class Context {

  private final View view;
  private final Controller controller;

  public Context(ConnectionSource source) {
    this.view = new View(source);
    this.controller = new Controller(source);
  }

  public UserContext create(String name) {
    final User user = controller.newUser(name);
    return user == null ?
        null :
        new UserContext(user, view, controller);
  }

  public Iterable<UserContext> allUsers() {
    final Collection<UserContext> users = new ArrayList<>();
    for (final User user : view.getUsers()) {
      users.add(new UserContext(user, view, controller));
    }
    return users;

  }
  
  public InterestSet getInterestSet(Uuid id){
  	return view.getInterestSet(id);
  }
  
  public void updateInterests(Uuid id, InterestSet intSet){
    controller.updateInterests(id, intSet);
  } 
  
  /**
   * Get the info of the server; version info should be returned currently.
   * @return The infomation of the server. If fails, null will be returned.
   */
  public ServerInfo getInfo() {
      return view.getInfo();
  }
}
