package com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.provider.gemini;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.googlesource.gerrit.plugins.reviewai.aibackend.langchain.provider.FallbackTokenCountEstimator;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import dev.langchain4j.model.TokenCountEstimator;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class GeminiLangChainProviderTest {

  private final GeminiLangChainProvider provider = new GeminiLangChainProvider();

  @Before
  public void resetEstimatorCache() throws Exception {
    Field field = GeminiLangChainProvider.class.getDeclaredField("estimatorAvailable");
    field.setAccessible(true);
    field.set(null, null);
  }

  @Test
  public void fallsBackToCl100kWhenGeminiModelUnsupported() {
    Configuration config = Mockito.mock(Configuration.class);
    when(config.aiModel()).thenReturn("gemini-fake-model");
    when(config.aiToken()).thenReturn("dummy-token");

    Optional<TokenCountEstimator> estimator = provider.createTokenEstimator(config);

    assertTrue(estimator.isPresent());
    assertTrue(estimator.get() instanceof FallbackTokenCountEstimator);
  }
}
