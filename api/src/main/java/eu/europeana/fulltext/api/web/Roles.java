package eu.europeana.fulltext.api.web;

import eu.europeana.api.commons.definitions.vocabulary.Role;
import eu.europeana.api.commons.web.model.vocabulary.Operations;
import java.util.Arrays;

public enum Roles implements Role {
  ANONYMOUS(new String[] {Operations.RETRIEVE}),
  EDITOR(
      new String[] {Operations.RETRIEVE, Operations.CREATE, Operations.DELETE, Operations.UPDATE}),
  ADMIN(
      new String[] {
        Operations.RETRIEVE,
        Operations.CREATE,
        Operations.DELETE,
        Operations.UPDATE,
        Operations.ADMIN_ALL
      });

  final String[] operations;

  Roles(String[] operations) {
    this.operations = operations;
  }

  public String[] getOperations() {
    return operations;
  }

  @Override
  public String[] getPermissions() {
    return getOperations();
  }

  @Override
  public String getName() {
    return this.name();
  }

  /**
   * This method returns the api specific Role for the given role name
   *
   * @param name the name of user role
   * @return the user role
   */
  public static Role getRoleByName(String name) {
    return Arrays.stream(Roles.values())
        .filter(role -> role.name().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
  }
}
