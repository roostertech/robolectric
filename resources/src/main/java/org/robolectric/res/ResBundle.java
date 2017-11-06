package org.robolectric.res;

import com.google.common.base.Strings;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.robolectric.res.android.ConfigDescription;
import org.robolectric.res.android.ResTable_config;

public class ResBundle {
  private final ResMap valuesMap = new ResMap();

  public void put(ResName resName, TypedResource value) {
    valuesMap.put(resName, value);
  }

  public TypedResource get(ResName resName, String qualifiers) {
    return valuesMap.pick(resName, qualifiers);
  }

  public void receive(ResourceTable.Visitor visitor) {
    for (final Map.Entry<ResName, Map<String, TypedResource>> entry : valuesMap.map.entrySet()) {
      visitor.visit(entry.getKey(), entry.getValue().values());
    }
  }

  static class ResMap {
    private final Map<ResName, Map<String, TypedResource>> map = new HashMap<>();

    public TypedResource pick(ResName resName, String qualifiersStr) {
      Map<String, TypedResource> values = map.get(resName);
      if (values == null || values.size() == 0) return null;

      Collection<TypedResource> typedResources = values.values();

      ResTable_config toMatch = new ResTable_config();
      if (!Strings.isNullOrEmpty(qualifiersStr) && !new ConfigDescription()
          .parse(qualifiersStr, toMatch)) {
        throw new IllegalArgumentException("Invalid qualifiers \"" + qualifiersStr + "\"");
      };

      TypedResource bestMatchSoFar = null;
      for (TypedResource candidate : typedResources) {
        ResTable_config candidateConfig = candidate.getConfig();
        if (candidateConfig.match(toMatch)) {
          if (bestMatchSoFar == null || candidateConfig.isBetterThan(bestMatchSoFar.getConfig(), toMatch)) {
            bestMatchSoFar = candidate;
          }
        }
      }

      return bestMatchSoFar;
    }

    public void put(ResName resName, TypedResource value) {
      Map<String, TypedResource> values = map.get(resName);
      if (values == null) map.put(resName, values = new HashMap<>());
      if (!values.containsKey(value.getXmlContext().getQualifiers())) {
        values.put(value.getXmlContext().getQualifiers(), value);
      }
    }

    public int size() {
      return map.size();
    }
  }
}
