package com.example.literakiapp.data.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

@Suppress("unused")
class ApiMoveTileJsonAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): ApiMoveTile {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> ApiMoveTile(letter = reader.nextString())
            JsonReader.Token.BEGIN_OBJECT -> readObject(reader)
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                throw JsonDataException("Move tile cannot be null at path ${reader.path}")
            }
            else -> throw JsonDataException(
                "Expected string or object but was ${reader.peek()} at path ${reader.path}"
            )
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: ApiMoveTile) {
        if (value.x == null || value.y == null) {
            writer.value(value.letter)
            return
        }

        writer.beginObject()
        writer.name("letter").value(value.letter)
        writer.name("x").value(value.x.toLong())
        writer.name("y").value(value.y.toLong())
        writer.endObject()
    }

    private fun readObject(reader: JsonReader): ApiMoveTile {
        var letter: String? = null
        var x: Int? = null
        var y: Int? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "letter" -> letter = reader.nextString()
                "x" -> x = readOptionalInt(reader)
                "y" -> y = readOptionalInt(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return ApiMoveTile(
            letter = letter ?: throw JsonDataException(
                "Missing letter in move tile at path ${reader.path}"
            ),
            x = x,
            y = y
        )
    }

    private fun readOptionalInt(reader: JsonReader): Int? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                null
            }
            JsonReader.Token.NUMBER -> reader.nextInt()
            else -> throw JsonDataException(
                "Expected number or null but was ${reader.peek()} at path ${reader.path}"
            )
        }
    }
}

