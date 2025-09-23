package com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.provider.moonshot;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.provider.FallbackTokenCountEstimator;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import dev.langchain4j.model.TokenCountEstimator;
import java.util.Optional;
import org.junit.Test;
import org.mockito.Mockito;

public class MoonshotLangChainProviderTest {

  private final MoonshotLangChainProvider provider = new MoonshotLangChainProvider();

  @Test
  public void fallsBackToCl100kWhenMoonshotModelUnsupported() {
    Configuration config = Mockito.mock(Configuration.class);
    when(config.getAiModel()).thenReturn("moonshot-v1-8k");

    Optional<TokenCountEstimator> estimator = provider.createTokenEstimator(config);

    assertTrue(estimator.isPresent());
    assertTrue(estimator.get() instanceof FallbackTokenCountEstimator);
  }
}
