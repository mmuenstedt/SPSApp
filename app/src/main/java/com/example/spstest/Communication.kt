package com.example.spstest

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket


/*
 * Created on 15.06.2005
 */
/**
 * The communication definitions to provide easy-to-use fuctions for reading and
 * writing data to a Siemens S7 PLC via the IBHLink (Ethernet to S7-MPI
 * converter).
 *
 *
 * ===========================================================================
 *
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 *
 * This program is intendend to be a SAMPLE application, on how the IBHLink
 * Ethernet/MPI/Profibus converter may be used in combination with a Siemens S7
 * PLC. If you plan to use this code (or parts of it) in your application, it is
 * completely up to you to verify wheter it may be suitable for your needs.<br></br>
 * Absolutely no resposibility can be taken by IBHsoftec GmbH for damages caused
 * by using this (or parts) of this code. As mentioned above, It's just a
 * sample.
 *
 * @author Mark Prößdorf, IBH softec GmbH
 * @version 1.00
 * @since 06.07.2005
 */
class Communication
/**
 * default constructor
 *
 */
{
    /*
	 * setup variables and objects
	 */
    private var connection: Socket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var messageNr: Byte = 0
    private var plcMpiAddress: Byte = 0


    fun connect() {
        val addr: InetAddress = InetAddress.getByName("172.22.15.119")
        try {
            if (!hasConnection()) {
                newConnection(addr, 2.toByte())
            }
        } catch (
            ioexc: IOException
        ) {
            ioexc.printStackTrace()
            Log.d("connectSPS", "Connection failed")
        }
    }

    fun setValue(type: String, nr: Int, dbnr: Int, bitnumber: Int, value: String): Boolean {
        try {
            if (!hasConnection()) {
                Log.e("setValue", "No Connection")
                return false
            }
            when (type) {
                "Integer" -> SetDBW(nr, dbnr, value.toInt())
                "Double Integer" -> SetDBD(nr, dbnr, value.toInt())
                "Byte" -> SetDBB(nr, dbnr, value.toInt())
                "Bit" -> SetDBX(nr, dbnr, bitnumber, value == "true" || value == "1")
                "Real" -> SetDBR(nr, dbnr, value.toFloat())
                else -> return false
            }
            Log.d("setValue", "Success")
            return true
        } catch (ioexc: IOException) {
            ioexc.printStackTrace()
            return false
        }
    }

    /**
     * method returns if Communication has connection
     *
     * @return connected
     */
    fun hasConnection(): Boolean {
        if (connection == null) {
            return false
        }
        return connection!!.isConnected
    }

    /**
     * method establishes new connection
     *
     * @param address:    ip to connect to
     * @param mpiAddress: MPI/DP-Address
     * @throws IOException: if connection can't be established
     */
    @Throws(IOException::class)
    fun newConnection(address: InetAddress?, mpiAddress: Byte) {
        if (connection != null && hasConnection()) {
            closeConnection()
        } // if a connection exist close it
        connection = Socket()
        connection!!.connect(
            InetSocketAddress(address, IBHLinkMSG.IBHLINK_PORT),
            2000
        ) // establish new TCP/IP connection
        input = connection!!.getInputStream() // setup new inputStream
        output = connection!!.getOutputStream() // setup new outputStream
        plcMpiAddress = mpiAddress
        messageNr = 0
        try {
            val status = GetM(0, 0) // read flag 0.0 to test MPI/DP-Address
            Log.d("newConnection", "connectionstatus: $status")
        } catch (ioexc: IOException) {
            closeConnection()
            throw ioexc // throw exception upwards
        }
    }

    /**
     * method close actual connection
     *
     * @throws IOException: if connection can't close
     */
    @Throws(IOException::class)
    fun closeConnection() {
        if (connection != null && hasConnection()) {
            try {
                PLCDisconnect()
            } catch (ioexc: IOException) {
            } // inside brackets is no code because the connection will be closed anyway
            connection!!.close()
            messageNr = 0
            plcMpiAddress = 0
            connection = null
            input = null
            output = null
        }
    }

    /**
     * method send and receives data
     *
     * @param data: byteArray which contains data to send
     * @return received data
     * @throws IOException: if data couldn't be sent or readed or timed out
     */
    @Synchronized
    @Throws(IOException::class)
    private fun sendReceiveData(data: ByteArray): ByteArray {
        var data = data
        output!!.write(data) // send data
        val tempData = ByteArray(0x100) // setup byteArray with enough space
        if (input!!.available() == 0) { // if there is no data
            for (i in 0..99) {
                try {
                    Thread.sleep(10) // wait 10 mili seconds
                } catch (iexc: InterruptedException) {
                }
                if (input!!.available() != 0) // if data arrives break loop
                    break
            }
            if (input!!.available() == 0) throw IOException("no answer")
        }
        input!!.read(tempData) // read data
        if (tempData[2] < 0) {
            data = ByteArray(0x100 + tempData[2] + 8)
        } else {
            data = ByteArray(tempData[2] + 8)
        }
        // setup byteArray with length =
        // header(8) + byte which
        // represents ln (if negative
        // build K2)
        for (i in data.indices) data[i] = tempData[i] // copy tempData into data
        return data
    }

    /**
     * method reads data from SPS
     *
     * @param Typ:  type of periphery
     * @param Nr:   address number of periphery, offset number of datablock
     * @param DBNr: address of datablock
     * @param size: number of bytes
     * @return readed data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun ReadVals(Typ: Char, Nr: Int, DBNr: Int, size: Int): ShortArray? {
        if (size <= 0) throw IOException("size out of bounds")
        val msg = IBHLinkMSG()
        msg.rx = IBHLinkMSG.MPI_TASK
        msg.tx = IBHLinkMSG.HOST
        msg.ln = IBHLinkMSG.MSG_PARAM_LEN
        msg.nr = messageNr
        msg.device_adr = plcMpiAddress
        msg.function = IBHLinkMSG.TASK_TFC_READ
        var maxCnt = IBHLinkMSG.IBHLINK_READ_MAX.toInt()
        if ((Typ == 'T') or (Typ == 'Z')) maxCnt /= 2
        checkType(maxCnt, msg, Typ, Nr, DBNr, size)
        messageNr++
        val dataStream = IBHLinkMSG(sendReceiveData(msg.toByteArray()))
        checkReturn(dataStream, msg)
        return dataStream.d
    }

    /**
     * method writes data into SPS
     *
     * @param Typ:  type of periphery
     * @param Nr:   address number of periphery, offset number of datablock
     * @param DBNr: address of datablock
     * @param size: number of bytes
     * @param data: data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun WriteVals(Typ: Char, Nr: Int, DBNr: Int, size: Int, data: ShortArray?) {
        if (size <= 0) throw IOException("size out of bounds")
        val msg = IBHLinkMSG()
        msg.rx = IBHLinkMSG.MPI_TASK
        msg.tx = IBHLinkMSG.HOST
        msg.nr = messageNr
        msg.device_adr = plcMpiAddress
        msg.function = IBHLinkMSG.TASK_TFC_WRITE
        msg.d = data
        var maxCnt = IBHLinkMSG.IBHLINK_WRITE_MAX.toInt()
        if ((Typ == 'T') or (Typ == 'Z')) maxCnt /= 2
        checkType(maxCnt, msg, Typ, Nr, DBNr, size)
        msg.ln =
            (IBHLinkMSG.MSG_PARAM_LEN + msg.data_cnt + if (Typ == 'T' || Typ == 'Z') msg.data_cnt else 0).toByte()
        messageNr++
        val dataStream = IBHLinkMSG(sendReceiveData(msg.toByteArray()))
        checkReturn(dataStream, msg)
    }

    /**
     * method sends disconnect message towards IBH_Link
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    fun PLCDisconnect() {
        val msg = IBHLinkMSG()
        msg.rx = IBHLinkMSG.MPI_TASK
        msg.tx = IBHLinkMSG.HOST
        msg.ln = IBHLinkMSG.MSG_SIZE
        msg.nr = messageNr
        msg.b = IBHLinkMSG.MPI_DISCONNECT
        msg.device_adr = plcMpiAddress
        messageNr++
        val dataStream = IBHLinkMSG(sendReceiveData(msg.toByteArray()))
        checkReturn(dataStream, msg)
    }

    /**
     * method reads status of SPS
     *
     * @return readed status
     * @throws IOException
     */
    @Throws(IOException::class)
    fun PLCGetRun(): Int {
        val msg = IBHLinkMSG()
        msg.rx = IBHLinkMSG.MPI_TASK
        msg.tx = IBHLinkMSG.HOST
        msg.ln = IBHLinkMSG.MSG_SIZE
        msg.nr = messageNr
        msg.b = IBHLinkMSG.MPI_GET_OP_STATUS
        msg.device_adr = plcMpiAddress
        messageNr++
        val dataStream = IBHLinkMSG(sendReceiveData(msg.toByteArray()))
        checkReturn(dataStream, msg)
        return dataStream.d!![0].toInt()
    }

    /**
     * method reads MW
     *
     * @param Nr: number of MW
     * @return readed data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetMW(Nr: Int): Int {
        val result: Int
        val readedData = ReadVals('M', Nr, 0, 2)
        result = readedData!![0] * 0x100 + readedData[1]
        return result
    }

    /**
     * method reads MB
     *
     * @param Nr: number of MB
     * @return readed data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetMB(Nr: Int): Int {
        val result: Int
        val readedData = ReadVals('M', Nr, 0, 1)
        result = readedData!![0].toInt()
        return result
    }

    /**
     * method reads MD
     *
     * @param Nr: number of MD
     * @return readed data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetMD(Nr: Int): Int {
        val result: Int
        val readedData = ReadVals('M', Nr, 0, 4)
        result =
            readedData!![0] * 0x1000000 + readedData[1] * 0x10000 + readedData[2] * 0x100 + readedData[3]
        return result
    }

    /**
     * method reads M
     *
     * @param Nr:    number of M
     * @param BitNr: bitnumber of M
     * @return readed data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetM(Nr: Int, BitNr: Int): Boolean {
        val result: Boolean
        val readedData = ReadVals('M', Nr, 0, 1)
        var digit = 1
        digit = digit shl BitNr
        result = readedData!![0].toInt() and digit == digit
        return result
    }

    /**
     * method writes MB
     *
     * @param Nr:   number of MB
     * @param data: data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetMB(Nr: Int, data: Int) {
        WriteVals('M', Nr, 0, 1, shortArrayOf(data.toByte().toShort()))
    }

    /**
     * method writes MW
     *
     * @param Nr:   number of MW
     * @param data: data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetMW(Nr: Int, data: Int) {
        var data = data
        data = if (data.toShort() < 0) 0x10000 + data.toShort() else data.toShort().toInt()
        val vals = ShortArray(2)
        vals[0] = (data / 0x100).toByte().toShort()
        vals[1] = (data and 0xFF).toByte().toShort()
        WriteVals('M', Nr, 0, 2, vals)
    }

    /**
     * method writes MD
     *
     * @param Nr:   number of MW
     * @param data: data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetMD(Nr: Int, data: Int) {
        val dat = if (data < 0) 0x100000000L + data else data.toLong()
        val vals = ShortArray(4)
        vals[0] = (dat / 0x1000000).toByte().toShort()
        vals[1] = (dat % 0x1000000 / 0x10000).toByte().toShort()
        vals[2] = (dat % 0x10000 / 0x100).toByte().toShort()
        vals[3] = (dat % 0x100).toByte().toShort()
        WriteVals('M', Nr, 0, 4, vals)
    }

    /**
     * method writes M
     *
     * @param Nr:    number of M
     * @param BitNr: bitnumber of M
     * @param data:  data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetM(Nr: Int, BitNr: Int, data: Boolean) {
        var stored = ReadVals('M', Nr, 0, 1)!![0]
        var digit = 1
        digit = digit shl BitNr
        val actual = stored.toInt() and digit == digit
        if (!actual) {
            val vals: ShortArray
            stored = (stored.toInt() xor digit).toShort()
            vals = shortArrayOf(stored)
            WriteVals('M', Nr, 0, 1, vals)
        }
    }

    /**
     * method reads EW
     *
     * @param Nr: number of EW
     * @return readed data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetEW(Nr: Int): Int {
        val result: Int
        val readedData = ReadVals('E', Nr, 0, 2)
        result = readedData!![0] * 0x100 + readedData[1]
        return result
    }

    /**
     * method reads EB
     *
     * @param Nr: number of EB
     * @return readed data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetEB(Nr: Int): Int {
        val result: Int
        val readedData = ReadVals('E', Nr, 0, 1)
        result = readedData!![0].toInt()
        return result
    }

    /**
     * method reads ED
     *
     * @param Nr: number of ED
     * @return readed data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetED(Nr: Int): Int {
        val result: Int
        val readedData = ReadVals('E', Nr, 0, 4)
        result =
            readedData!![0] * 0x1000000 + readedData[1] * 0x10000 + readedData[2] * 0x100 + readedData[3]
        return result
    }

    /**
     * method reads E
     *
     * @param Nr:    number of E
     * @param BitNr: bitnumber of E
     * @return readed data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetE(Nr: Int, BitNr: Int): Boolean {
        val result: Boolean
        val readedData = ReadVals('E', Nr, 0, 1)
        var digit = 1
        digit = digit shl BitNr
        result = readedData!![0].toInt() and digit == digit
        return result
    }

    /**
     * method writes EB
     *
     * @param Nr:   number of EB
     * @param data: data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetEB(Nr: Int, data: Int) {
        WriteVals('E', Nr, 0, 1, shortArrayOf(data.toByte().toShort()))
    }

    /**
     * method to write EW
     *
     * @param Nr:   number of EW
     * @param data: data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetEW(Nr: Int, data: Int) {
        var data = data
        data = if (data.toShort() < 0) 0x10000 + data.toShort() else data.toShort().toInt()
        val vals = ShortArray(2)
        vals[0] = (data / 0x100).toByte().toShort()
        vals[1] = (data and 0xFF).toByte().toShort()
        WriteVals('E', Nr, 0, 2, vals)
    }

    /**
     * method to write ED
     *
     * @param Nr:   number of ED
     * @param data: data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetED(Nr: Int, data: Int) {
        val dat = if (data < 0) 0x100000000L + data else data.toLong()
        val vals = ShortArray(4)
        vals[0] = (dat / 0x1000000).toByte().toShort()
        vals[1] = (dat % 0x1000000 / 0x10000).toByte().toShort()
        vals[2] = (dat % 0x10000 / 0x100).toByte().toShort()
        vals[3] = (dat % 0x100).toByte().toShort()
        WriteVals('E', Nr, 0, 4, vals)
    }

    /**
     * method to write E
     *
     * @param Nr:    number of E
     * @param BitNr: bitnumber of E
     * @param data:  data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetE(Nr: Int, BitNr: Int, data: Boolean) {
        var stored = ReadVals('E', Nr, 0, 1)!![0]
        var digit = 1
        digit = digit shl BitNr
        val actual = stored.toInt() and digit == digit
        if (!actual) {
            val vals: ShortArray
            stored = (stored.toInt() xor digit).toShort()
            vals = shortArrayOf(stored)
            WriteVals('E', Nr, 0, 1, vals)
        }
    }

    /**
     * method to read AW
     *
     * @param Nr: number of AW
     * @return readed data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetAW(Nr: Int): Int {
        val result: Int
        val readedData = ReadVals('A', Nr, 0, 2)
        result = readedData!![0] * 0x100 + readedData[1]
        return result
    }

    /**
     * method to read AB
     *
     * @param Nr: number of AB
     * @return readed data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetAB(Nr: Int): Int {
        val result: Int
        val readedData = ReadVals('A', Nr, 0, 1)
        result = readedData!![0].toInt()
        return result
    }

    /**
     * method to read AD
     *
     * @param Nr: number of AD
     * @return readed data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetAD(Nr: Int): Int {
        val result: Int
        val readedData = ReadVals('A', Nr, 0, 4)
        result =
            readedData!![0] * 0x1000000 + readedData[1] * 0x10000 + readedData[2] * 0x100 + readedData[3]
        return result
    }

    /**
     * method to read A
     *
     * @param Nr:    number of A
     * @param BitNr: bitnumber of A
     * @return readed data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetA(Nr: Int, BitNr: Int): Boolean {
        val result: Boolean
        val readedData = ReadVals('A', Nr, 0, 1)
        var digit = 1
        digit = digit shl BitNr
        result = readedData!![0].toInt() and digit == digit
        return result
    }

    /**
     * method to write AB
     *
     * @param Nr:   number of AB
     * @param data: data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetAB(Nr: Int, data: Int) {
        WriteVals('A', Nr, 0, 1, shortArrayOf(data.toByte().toShort()))
    }

    /**
     * method to write AW
     *
     * @param Nr:   number of AW
     * @param data: data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetAW(Nr: Int, data: Int) {
        var data = data
        data = if (data.toShort() < 0) 0x10000 + data.toShort() else data.toShort().toInt()
        val vals = ShortArray(2)
        vals[0] = (data / 0x100).toByte().toShort()
        vals[1] = (data and 0xFF).toByte().toShort()
        WriteVals('A', Nr, 0, 2, vals)
    }

    /**
     * method to write AD
     *
     * @param Nr:   number of AD
     * @param data: data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetAD(Nr: Int, data: Int) {
        val dat = if (data < 0) 0x100000000L + data else data.toLong()
        val vals = ShortArray(4)
        vals[0] = (dat / 0x1000000).toByte().toShort()
        vals[1] = (dat % 0x1000000 / 0x10000).toByte().toShort()
        vals[2] = (dat % 0x10000 / 0x100).toByte().toShort()
        vals[3] = (dat % 0x100).toByte().toShort()
        WriteVals('A', Nr, 0, 4, vals)
    }

    /**
     * method to write A
     *
     * @param Nr:    number of A
     * @param BitNr: bitnumber of A
     * @param data:  data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetA(Nr: Int, BitNr: Int, data: Boolean) {
        var stored = ReadVals('A', Nr, 0, 1)!![0]
        var digit = 1
        digit = digit shl BitNr
        val actual = stored.toInt() and digit == if (data) digit else 0
        if (!actual) {
            val vals: ShortArray
            stored = (stored.toInt() xor digit).toShort()
            vals = shortArrayOf(stored)
            WriteVals('A', Nr, 0, 1, vals)
        }
    }

    /**
     * method to read DBW
     *
     * @param Nr:   offset number of DBW
     * @param DBNr: number of DB
     * @return readed data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetDBW(Nr: Int, DBNr: Int): Int {
        val result: Int
        val readedData = ReadVals('D', Nr, DBNr, 2)
        result = readedData!![0] * 0x100 + readedData[1]
        return result
    }

    /**
     * method to read DB Real
     *
     * @param Nr:   offset number of DBR
     * @param DBNr: number of DB
     * @return readed data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetDBR(Nr: Int, DBNr: Int): Float {
        val result: Int
        val readedData = ReadVals('D', Nr, DBNr, 4)
        result =
            readedData!![0] * 0x1000000 + readedData[1] * 0x10000 + readedData[2] * 0x100 + readedData[3]
        return java.lang.Float.intBitsToFloat(result)
    }

    /**
     * method to read DBB
     *
     * @param Nr:   offset number of DBB
     * @param DBNr: number of DB
     * @return read data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetDBB(Nr: Int, DBNr: Int): Int {
        val result: Int
        val readedData = ReadVals('D', Nr, DBNr, 1)
        result = readedData!![0].toInt()
        return result
    }

    /**
     * method to read DBD
     *
     * @param Nr:   offset number of DBD
     * @param DBNr: number of DB
     * @return read data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetDBD(Nr: Int, DBNr: Int): Int {
        val result: Int
        val readedData = ReadVals('D', Nr, DBNr, 4)
        result =
            readedData!![0] * 0x1000000 + readedData[1] * 0x10000 + readedData[2] * 0x100 + readedData[3]
        return result
    }

    /**
     * method to read DBX
     *
     * @param Nr:    offset number of DBX
     * @param DBNr:  number of DB
     * @param BitNr: bitnumber of DBX
     * @return read data
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetDBX(Nr: Int, DBNr: Int, BitNr: Int): Boolean {
        val result: Boolean
        val readedData = ReadVals('D', Nr, DBNr, 1)
        var digit = 1
        digit = digit shl BitNr
        result = readedData!![0].toInt() and digit == digit
        return result
    }

    /**
     * method to write DBB
     *
     * @param Nr:   offset number of DBB
     * @param DBNr: number of DB
     * @param data: data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetDBB(Nr: Int, DBNr: Int, data: Int) {
        WriteVals('D', Nr, DBNr, 1, shortArrayOf(data.toByte().toShort()))
    }

    /**
     * method to write DBW
     *
     * @param Nr:   offset number of DBW
     * @param DBNr: number of DB
     * @param data: data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetDBW(Nr: Int, DBNr: Int, data: Int) {
        var data = data
        data = if (data.toShort() < 0) 0x10000 + data.toShort() else data.toShort().toInt()
        val vals = ShortArray(2)
        vals[0] = (data / 0x100).toByte().toShort()
        vals[1] = (data and 0xFF).toByte().toShort()
        WriteVals('D', Nr, DBNr, 2, vals)
    }

    /**
     * method to write DBD
     *
     * @param Nr:   offset number of DBD
     * @param DBNr: number of DB
     * @param data: data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetDBD(Nr: Int, DBNr: Int, data: Int) {
        val dat = if (data < 0) 0x100000000L + data else data.toLong()
        val vals = ShortArray(4)
        vals[0] = (dat / 0x1000000).toByte().toShort()
        vals[1] = (dat % 0x1000000 / 0x10000).toByte().toShort()
        vals[2] = (dat % 0x10000 / 0x100).toByte().toShort()
        vals[3] = (dat % 0x100).toByte().toShort()
        WriteVals('D', Nr, DBNr, 4, vals)
    }

    /**
     * method to write DBR
     *
     * @param Nr:   offset number of DBR
     * @param DBNr: number of DB
     * @param data: data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetDBR(Nr: Int, DBNr: Int, data: Float) {
        val intBits = java.lang.Float.floatToIntBits(data)
        val vals = ShortArray(4)
        vals[0] = (intBits ushr 24).toByte().toShort()
        vals[1] = (intBits ushr 16 and 0xFF).toByte().toShort()
        vals[2] = (intBits ushr 8 and 0xFF).toByte().toShort()
        vals[3] = (intBits and 0xFF).toByte().toShort()
        WriteVals('D', Nr, DBNr, 4, vals)
    }

    /**
     * method to write DBX
     *
     * @param Nr:    offset number of DBX
     * @param DBNr:  number of DB
     * @param BitNr: bitnumber of DBX
     * @param data:  data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    fun SetDBX(Nr: Int, DBNr: Int, BitNr: Int, data: Boolean) {
        var stored = ReadVals('D', Nr, DBNr, 1)!![0]
        var digit = 1
        digit = digit shl BitNr

        val vals: ShortArray
        stored = (stored.toInt() xor digit).toShort()
        vals = shortArrayOf(stored)
        WriteVals('D', Nr, DBNr, 1, vals)

    }

    /**
     * method to read counters
     *
     * @param Nr:     number of counter
     * @param Anzahl: size of counters
     * @return readed counters
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetZ(Nr: Int, Anzahl: Int): IntArray {
        val data = IntArray(Anzahl)
        val results = ReadVals('Z', Nr, 0, Anzahl)
        for (i in results!!.indices) data[i] = if (results[i] < 0) 512 + results[i] else results[i]
            .toInt()
        return data
    }

    /**
     * method to read counter
     *
     * @param Nr: number of counter
     * @return readed counter
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetZ(Nr: Int): Int {
        return GetZ(Nr, 1)[0]
    }

    /**
     * method to read timers
     *
     * @param Nr:     number of timer
     * @param Anzahl: size of timers
     * @return readed timers
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetT(Nr: Int, Anzahl: Int): IntArray {
        val data = IntArray(Anzahl)
        val results = ReadVals('T', Nr, 0, Anzahl)
        for (i in results!!.indices) data[i] = if (results[i] < 0) 512 + results[i] else results[i]
            .toInt()
        return data
    }

    /**
     * method to read timer
     *
     * @param Nr: number of timer
     * @return readed timer
     * @throws IOException
     */
    @Throws(IOException::class)
    fun GetT(Nr: Int): Int {
        return GetT(Nr, 1)[0]
    }

    /**
     * method to change IEEE into MC7
     *
     * @param val: IEEE number
     * @return MC7 number
     */
    fun IEEEtoMC7(`val`: Float): Int {
        var cnv = `val`.toInt()
        val data = ShortArray(4)
        data[0] = (cnv / 0x1000000).toShort()
        data[1] = (cnv % 0x1000000 / 0x10000).toShort()
        data[2] = (cnv % 0x10000 / 0x100).toShort()
        data[3] = (cnv % 0x100).toShort()
        if (data[0] < 0) data[0] = (0x100 + data[0]).toShort()
        cnv = (data[3] * 0x1000000 + data[2] * 0x10000 + data[1] * 0x100 + data[0])
        return cnv
    }

    /**
     * method to change MC7 into IEEE
     *
     * @param val: MC7 number
     * @return IEEE number
     */
    fun MC7toIEEE(`val`: Int): Float {
        var `val` = `val`
        val data = ShortArray(4)
        data[0] = (`val` / 0x1000000).toShort()
        data[1] = (`val` % 0x1000000 / 0x10000).toShort()
        data[2] = (`val` % 0x10000 / 0x100).toShort()
        data[3] = (`val` % 0x100).toShort()
        if (data[0] < 0) data[0] = (0x100 + data[0]).toShort()
        `val` =
            (data[3].toLong() * 0x1000000 + data[2] * 0x10000 + data[1] * 0x100 + data[0]).toInt()
        return `val`.toFloat()
    }

    /**
     * method to check if size is inside spezifications
     *
     * @param READ_WRITE: size of maximal data to write
     * @param msg:        object to setup
     * @param Typ:        type of periphery
     * @param Nr:         address number of periphery
     * @param DBNr:       datablock number
     * @param size:       size of data to write
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun checkType(
        READ_WRITE: Int,
        msg: IBHLinkMSG,
        Typ: Char,
        Nr: Int,
        DBNr: Int,
        size: Int
    ) {
        when (Typ) {
            'E' -> {
                if (size > READ_WRITE) throw IOException("size out of bounds")
                msg.b = IBHLinkMSG.MPI_READ_WRITE_IO
                msg.data_area = IBHLinkMSG.INPUT_AREA
                msg.data_adr = (Nr and 0xFFFF).toShort()
                msg.data_cnt = (size and 0xFF).toByte()
                msg.data_type = IBHLinkMSG.TASK_TDT_UINT8
            }

            'A' -> {
                if (size > READ_WRITE) throw IOException("size out of bounds")
                msg.b = IBHLinkMSG.MPI_READ_WRITE_IO
                msg.data_area = IBHLinkMSG.OUTPUT_AREA
                msg.data_adr = (Nr and 0xFFFF).toShort()
                msg.data_cnt = (size and 0xFF).toByte()
                msg.data_type = IBHLinkMSG.TASK_TDT_UINT8
            }

            'M' -> {
                if (size > READ_WRITE) throw IOException("size out of bounds")
                msg.b = IBHLinkMSG.MPI_READ_WRITE_M
                msg.data_adr = (Nr and 0xFFFF).toShort()
                msg.data_cnt = (size and 0xFF).toByte()
                msg.data_type = IBHLinkMSG.TASK_TDT_UINT8
            }

            'D' -> {
                if (size > READ_WRITE) throw IOException("size out of bounds")
                msg.b = IBHLinkMSG.MPI_READ_WRITE_DB
                msg.data_area = ((Nr and 0xFF00) / 0x100).toByte()
                msg.data_adr = (DBNr and 0xFFFF).toShort()
                msg.data_idx = (Nr and 0xFF).toByte()
                msg.data_cnt = (size and 0xFF).toByte()
                msg.data_type = IBHLinkMSG.TASK_TDT_UINT8
            }

            'T' -> {
                if (size > READ_WRITE / 2) throw IOException("size out of bounds")
                msg.b = IBHLinkMSG.MPI_READ_WRITE_TIM
                msg.data_adr = (Nr and 0xFFFF).toShort()
                msg.data_cnt = (size and 0xFF).toByte()
                msg.data_type = IBHLinkMSG.TASK_TDT_UINT16
            }

            'Z' -> {
                if (size > READ_WRITE / 2) throw IOException("size out of bounds")
                msg.b = IBHLinkMSG.MPI_READ_WRITE_CNT
                msg.data_adr = (Nr and 0xFFFF).toShort()
                msg.data_cnt = (size and 0xFF).toByte()
                msg.data_type = IBHLinkMSG.TASK_TDT_UINT16
            }

            else -> throw IOException("type out of index")
        }
    }

    /**
     * method to check if received data is valid
     *
     * @param dataStream: received object
     * @param msg:        sent object
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun checkReturn(dataStream: IBHLinkMSG, msg: IBHLinkMSG) {
        var consistentData = true
        if (dataStream.rx != IBHLinkMSG.HOST) consistentData = false
        if (dataStream.tx != IBHLinkMSG.MPI_TASK) consistentData = false
        if (dataStream.nr != msg.nr) consistentData = false
        if ((dataStream.a < IBHLinkMSG.MPI_READ_WRITE_DB) or (dataStream.a > IBHLinkMSG.MPI_READ_WRITE_TIM)) if (dataStream.a != IBHLinkMSG.MPI_DISCONNECT) consistentData =
            false
        if (dataStream.b.toInt() != 0) consistentData = false
        if (dataStream.e.toInt() != 0) consistentData = false
        if ((dataStream.device_adr < 0) or (dataStream.device_adr > 126)) consistentData = false
        when (dataStream.a) {
            IBHLinkMSG.MPI_READ_WRITE_DB -> {
                if (dataStream.data_type != IBHLinkMSG.TASK_TDT_UINT8) consistentData = false
                if ((dataStream.function < IBHLinkMSG.TASK_TFC_READ) or (dataStream.function > IBHLinkMSG.TASK_TFC_WRITE)) consistentData =
                    false
            }

            IBHLinkMSG.MPI_GET_OP_STATUS -> {
                if (dataStream.ln.toInt() != 10) consistentData = false
                if ((dataStream.data_area.toInt() != 0) or (dataStream.data_adr.toInt() != 0) or (dataStream.data_idx.toInt() != 0
                            ) or (dataStream.data_cnt.toInt() != 0) or (dataStream.data_type.toInt() != 0) or (dataStream.function.toInt() != 0)
                ) consistentData = false
            }

            IBHLinkMSG.MPI_READ_WRITE_M -> {
                if ((dataStream.data_area.toInt() != 0) or (dataStream.data_idx.toInt() != 0)) consistentData =
                    false
                if (dataStream.data_type != IBHLinkMSG.TASK_TDT_UINT8) consistentData = false
                if ((dataStream.function < IBHLinkMSG.TASK_TFC_READ) or (dataStream.function > IBHLinkMSG.TASK_TFC_WRITE)) consistentData =
                    false
            }

            IBHLinkMSG.MPI_READ_WRITE_IO -> {
                if ((dataStream.data_area < IBHLinkMSG.INPUT_AREA) or (dataStream.data_area > IBHLinkMSG.OUTPUT_AREA)) consistentData =
                    false
                if (dataStream.data_idx.toInt() != 0) consistentData = false
                if (dataStream.data_type != IBHLinkMSG.TASK_TDT_UINT8) consistentData = false
                if ((dataStream.function < IBHLinkMSG.TASK_TFC_READ) or (dataStream.function > IBHLinkMSG.TASK_TFC_WRITE)) consistentData =
                    false
            }

            IBHLinkMSG.MPI_READ_WRITE_CNT, IBHLinkMSG.MPI_READ_WRITE_TIM -> {
                if ((dataStream.data_area.toInt() != 0) or (dataStream.data_idx.toInt() != 0)) consistentData =
                    false
                if (dataStream.data_type != IBHLinkMSG.TASK_TDT_UINT16) consistentData = false
                if ((dataStream.function < IBHLinkMSG.TASK_TFC_READ) or (dataStream.function > IBHLinkMSG.TASK_TFC_WRITE)) consistentData =
                    false
            }

            IBHLinkMSG.MPI_DISCONNECT -> {
                if (dataStream.ln != IBHLinkMSG.MSG_PARAM_LEN) consistentData = false
                if ((dataStream.data_area.toInt() != 0) or (dataStream.data_adr.toInt() != 0) or (dataStream.data_idx.toInt() != 0
                            ) or (dataStream.data_cnt.toInt() != 0) or (dataStream.data_type.toInt() != 0) or (dataStream.function.toInt() != 0)
                ) consistentData = false
            }
        }
        if (!consistentData) throw IOException("no consistent data")
        when (dataStream.f) {
            IBHLinkMSG.CON_OK -> {}
            IBHLinkMSG.CON_UE -> throw IOException("timeout from remote station")
            IBHLinkMSG.CON_RR -> throw IOException("resource unavailable")
            IBHLinkMSG.CON_RS -> throw IOException("requested function of master is not activated within the remote station.")
            IBHLinkMSG.CON_NA -> throw IOException("no response of the remote station")
            IBHLinkMSG.CON_DS -> throw IOException("master not into the logical token ring")
            IBHLinkMSG.CON_LR -> throw IOException("Resource of the local FDL controller not available or not sifficient")
            IBHLinkMSG.CON_IV -> throw IOException("the specified msg.data_cnt parameter invalid")
            IBHLinkMSG.CON_TO -> throw IOException(
                "timeout, the request message was accepted but no inication is sent back by the remote station"
            )

            IBHLinkMSG.CON_SE -> throw IOException(
                "Seqence fault, internal state machine error. Remote station does not react like awaited or a reconnection is already open or device has no SAPs left to open connection channel"
            )

            IBHLinkMSG.REJ_IV -> throw IOException("specified offset address ut of limits or not known in the remote station")
            IBHLinkMSG.REJ_PDU -> throw IOException("wrong PDU coding in the MPI response of the remote station")
            IBHLinkMSG.REJ_OP -> throw IOException("specific length to write or to read results in an access otuside the limits")
            else -> throw IOException(
                "communication malfunction, not able to resolve Errorcode " + dataStream.f + " in dec"
            )
        }
    }
}