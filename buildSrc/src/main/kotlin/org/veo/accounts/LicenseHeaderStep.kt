/**
 * verinice.veo accounts
 * Copyright (C) 2018  Jochen Kemnade
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.veo.accounts

import com.diffplug.spotless.FormatterFunc
import com.diffplug.spotless.FormatterStep
import org.eclipse.jgit.api.Git
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.time.YearMonth
import java.util.Objects
import java.util.regex.Pattern

private const val NAME = "veoLicenseHeader"
private const val LICENSE_HEADER_DELIMITER = "package |@file"
private const val LICENSE_HEADER_TEMPLATE =
    "/**\n" +
            " * verinice.veo accounts\n" +
            " * Copyright (C) \$YEAR  \$AUTHOR\n" +
            " *\n" +
            " * This program is free software: you can redistribute it and/or modify\n" +
            " * it under the terms of the GNU Affero General Public License as published by\n" +
            " * the Free Software Foundation, either version 3 of the License, or\n" +
            " * (at your option) any later version.\n" +
            " *\n" +
            " * This program is distributed in the hope that it will be useful,\n" +
            " * but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
            " * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
            " * GNU Affero General Public License for more details.\n" +
            " *\n" +
            " * You should have received a copy of the GNU Affero General Public License\n" +
            " * along with this program.  If not, see <http://www.gnu.org/licenses/>.\n" +
            " */\n"

/** Prefixes a license header before the package statement. */
class LicenseHeaderStep private constructor(
    author: String,
    delimiter: String,
) : Serializable {
    private val author: String
    private val delimiterPattern: Pattern

    /** The license that we'd like enforced. */
    init {
        require(!delimiter.contains("\n")) { "The delimiter must not contain any newlines." }

        this.author = author
        this.delimiterPattern = Pattern.compile("^$delimiter", Pattern.UNIX_LINES or Pattern.MULTILINE)
    }

    /** Formats the given string. */
    fun format(raw: String): String {
        val matcher = delimiterPattern.matcher(raw)
        require(matcher.find()) { "Unable to find delimiter regex $delimiterPattern in $raw" }
        val existingLicense = raw.substring(0, matcher.start())
        if (existingLicense.contains("Apache Software License")) {
            // don't change files which have a different license
            return raw
        }
        val licenseTemplateWithTokensReplaced =
            LICENSE_HEADER_TEMPLATE
                .replace("\$YEAR", "\\E\\d{4}\\Q")
                .replace("\$AUTHOR", "\\E[\\p{IsAlphabetic}' -]+\\Q")
        val p =
            Pattern.compile(
                "^\\Q$licenseTemplateWithTokensReplaced\\E",
                Pattern.UNIX_LINES or Pattern.MULTILINE,
            )
        val m = p.matcher(existingLicense)
        if (m.find()) {
            // if no change is required, return the raw string without
            // creating any other new strings for maximum performance
            return raw
        } else {
            // otherwise we'll have to add the header
            val existingInfo = Pattern.compile("Copyright \\([cC]\\) (\\d{4}) +(.*)")
            val existingInfoMatcher = existingInfo.matcher(existingLicense)
            val yearToUse: String
            val authorToUse: String
            if (existingInfoMatcher.find()) {
                yearToUse = existingInfoMatcher.group(1)
                authorToUse = existingInfoMatcher.group(2)
            } else {
                yearToUse = YearMonth.now().year.toString()
                authorToUse = author
            }
            val licenseHeaderExtrapolated =
                LICENSE_HEADER_TEMPLATE.replace("\$YEAR", yearToUse).replace("\$AUTHOR", authorToUse)
            return licenseHeaderExtrapolated + raw.substring(matcher.start())
        }
    }

    companion object {
        @Throws(IOException::class)
        fun create(projectDirectory: File?): FormatterStep {
            Objects.requireNonNull<File?>(projectDirectory, "projectDirectory")
            val author =
                Git.open(projectDirectory).use { git ->
                    git.repository.config.getString("user", null, "name") ?: "<name>"
                }
            return create(author)
        }

        /** Creates a FormatterStep which forces the start of each file to match a license header. */
        fun create(author: String?): FormatterStep {
            Objects.requireNonNull<String?>(author, "author")
            return FormatterStep.create<LicenseHeaderStep?>(
                NAME,
                LicenseHeaderStep(author!!, LICENSE_HEADER_DELIMITER),
            ) { step: LicenseHeaderStep? ->
                FormatterFunc { raw: String? -> step!!.format(raw!!) }
            }
        }

        fun name(): String = NAME
    }
}