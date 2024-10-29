package com.example.spstest

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

class FileHelper {
    companion object {
        private const val FILE_NAME = "settings.json"

        fun saveJsonToFile(context: Context, jsonObject: JSONObject) {
            try {
                context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use { fos ->
                    fos.write(jsonObject.toString(2).toByteArray())
                }
            } catch (e: FileNotFoundException) {
                createFile(context)
            }
        }

        fun loadJsonFromFile(context: Context): JSONObject {
            //val file = File(context.filesDir, FILE_NAME)
            //file.delete()
            val jsonString = StringBuilder()
            try {
                context.openFileInput(FILE_NAME).use { fis ->
                    fis.bufferedReader().useLines { lines ->
                        lines.forEach { jsonString.append(it) }
                    }
                }
            } catch (e: FileNotFoundException) {
               createFile(context)
                return loadJsonFromFile(context)
            }


            return JSONObject(jsonString.toString())
        }

        private fun createFile(context: Context) {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) {
                file.createNewFile()

            }
            val initialJson = JSONObject()
            val exampleSetting = "[{\"name\": \"Pumpenfrequenz\", \"type\": \"Integer\", \"nr\": 0, \"dbnr\": 10, \"unit\": \"Hz\", \"factor\": 0.9615384615384615}]"
            initialJson.put("settings", JSONArray(exampleSetting))
            saveJsonToFile(context, initialJson)

        }
    }
}