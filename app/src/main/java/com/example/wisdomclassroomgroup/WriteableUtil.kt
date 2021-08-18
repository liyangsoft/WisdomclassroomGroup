package com.example.wisdomclassroomgroup

import com.example.wisdomclassroomgroup.io.DataInputStreamBuffer
import com.example.wisdomclassroomgroup.io.DataOutputStreamBuffer
import com.example.wisdomclassroomgroup.io.Writable


class WriteableUtil {
    companion object{
        fun toByteArray(w: Writable):ByteArray{
            val tmp = DataOutputStreamBuffer()
            w.write(tmp)
            tmp.flush()
            return tmp.toByteArray()
        }

        fun <T> parse(byteArray: ByteArray,clazz: Class<T>):T{
            val tmp = clazz.newInstance()
            val bytes = DataInputStreamBuffer(byteArray)
            tmp as Writable
            tmp.readFields(bytes)
            return tmp
        }
    }
}

fun Writable.toByteArray():ByteArray{
    return WriteableUtil.toByteArray(this)
}