package com.example.elisdb.data.local

import java.util.Locale

/**
 * Tags are stored in the single `tag` field as a `;`-separated list. The app
 * normalizes them to upper case, removes duplicates and sorts them so the
 * canonical value is stable across devices.
 */
fun tagsOf(raw: String?): List<String> =
  raw
    ?.split(';')
    ?.map { it.trim().uppercase(Locale.ROOT) }
    ?.filter { it.isNotEmpty() }
    ?.distinct()
    ?.sorted()
    .orEmpty()

/** Joins a list of tags into the canonical `;`-separated value. */
fun joinTags(tags: List<String>): String? =
  tags
    .map { it.trim().uppercase(Locale.ROOT) }
    .filter { it.isNotEmpty() }
    .distinct()
    .sorted()
    .takeIf { it.isNotEmpty() }
    ?.joinToString(";")

/** Maximum number of tags accepted per client. */
const val MAX_TAGS_PER_CLIENT = 10

/** Maximum length of a single tag. */
const val MAX_TAG_LENGTH = 100

/** Maximum length of the joined `;`-separated tag field. */
const val MAX_TAGS_FIELD_LENGTH = 500
