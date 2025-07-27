package org.macrobit.autocollapsegenerics.config

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import org.macrobit.autocollapsegenerics.config.ui.AutoCollapseGenericsSettingsConfigurable

class AutoCollapseGenericsConfigurable : ConfigurableProvider() {

  override fun createConfigurable(): Configurable? {
    return AutoCollapseGenericsSettingsConfigurable()
  }

}
