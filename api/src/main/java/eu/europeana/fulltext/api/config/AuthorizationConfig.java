package eu.europeana.fulltext.api.config;

import eu.europeana.api.commons.definitions.vocabulary.Role;
import eu.europeana.api.commons.nosql.service.ApiWriteLockService;
import eu.europeana.api.commons.oauth2.service.impl.EuropeanaClientDetailsService;
import eu.europeana.api.commons.service.authorization.BaseAuthorizationService;
import eu.europeana.fulltext.api.web.Roles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.ClientDetailsService;

@Configuration
public class AuthorizationConfig extends BaseAuthorizationService
    implements eu.europeana.api.commons.service.authorization.AuthorizationService {


  private final FTSettings appSettings;
  private final EuropeanaClientDetailsService clientDetailsService;

  @Autowired
  public AuthorizationConfig(
      FTSettings appSettings, EuropeanaClientDetailsService clientDetailsService) {
    this.appSettings = appSettings;

    this.clientDetailsService = clientDetailsService;
  }

  @Override
  protected ClientDetailsService getClientDetailsService() {
    return clientDetailsService;
  }

  @Override
  protected String getSignatureKey() {
    return appSettings.getApiKeyPublicKey();
  }

  @Override
  protected String getApiName() {
    return appSettings.getAuthorizationApiName();
  }

  @Override
  protected ApiWriteLockService getApiWriteLockService() {
    // TODO Auto-generated method stub
    return null;  }

  @Override
  protected Role getRoleByName(String name) {
    return Roles.getRoleByName(name);
  }

}
