package com.example.spstest

/*
 * Created on 15.06.2005
 */ /**
 * The communication definitions to provide easy-to-use fuctions for
 * reading and writing data to a Siemens S7 PLC via the IBHLink
 * (Ethernet to S7-MPI converter).
 *
 * ===========================================================================
 *
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 *
 * This program is intendend to be a SAMPLE application, on how the IBHLink
 * Ethernet/MPI/Profibus converter may be used in combination with a Siemens
 * S7 PLC. If you plan to use this code (or parts of it) in your
 * application, it is completely up to you to verify wheter it may be
 * suitable for your needs.<br></br>
 * Absolutely no resposibility can be taken by IBHsoftec GmbH for damages
 * caused by using this (or parts) of this code.
 * As mentioned above, It's just a sample.
 *
 * @author Mark Prößdorf, IBH softec GmbH
 * @version 1.00
 * @since 06.07.2005
 */
class IBHLinkMSG {
    /*
	 * setup variables
	 */
	@JvmField
	var rx: Byte
    @JvmField
	var tx: Byte
    @JvmField
	var ln: Byte
    @JvmField
	var nr: Byte
    @JvmField
	var a: Byte
    @JvmField
	var f: Byte
    @JvmField
	var b: Byte
    @JvmField
	var e: Byte
    @JvmField
	var device_adr: Byte
    @JvmField
	var data_area: Byte
    @JvmField
	var data_idx: Byte
    @JvmField
	var data_cnt: Byte
    @JvmField
	var data_type: Byte
    @JvmField
	var function: Byte
    @JvmField
	var data_adr: Short
    @JvmField
	var d: ShortArray?

    /**
     * default constructor
     *
     */
    internal constructor() {
        rx = HOST
        tx = MPI_TASK
        ln = 0
        nr = 0
        a = 0
        f = 0
        b = 0
        e = 0
        device_adr = 0
        data_area = 0
        data_idx = 0
        data_cnt = 0
        data_type = 0
        function = 0
        data_adr = 0
        d = null
    }

    /**
     * constructor to create object from returned data
     *
     * @param data: returned data from IBH_Link
     */
    internal constructor(data: ByteArray) {
        rx = data[0]
        tx = data[1]
        ln = data[2]
        nr = data[3]
        a = data[4]
        f = data[5]
        b = data[6]
        e = data[7]
        device_adr = data[8]
        data_area = data[9]
        //assemble data_adr from 2 bytes data
        var data_area_temp = data[11] * 0x100
        if (data[10] < 0) {
            data_area_temp += (0x100 + data[10])
        } else {
            data_area_temp += data[10]
        }
        data_adr =data_area_temp.toShort()
        data_idx = data[12]
        data_cnt = data[13]
        data_type = data[14]
        function = data[15]
        val length =
            (if (ln < 0) 0x100 + ln else ln).toShort() //assemble length from possible K2 data
        if (length > 8) {    //if data exists
            if (data_type.toInt() == 5) {
                d = ShortArray(length - 8)
                for (i in d!!.indices) d!![i] =
                    (if (data[16 + i] < 0) 0x100 + data[16 + i] else data[16 + i]).toShort() //assemble d from possible K2 data
            } else {
                if (a == MPI_GET_OP_STATUS) {
                    var d_temp = data[17] * 0x100
                    if (data[16] < 0) {
                        d_temp += (0x100 + data[16])
                    } else{
                        d_temp += data[16]
                    }
                    d =
                        shortArrayOf(d_temp.toShort()) //assemble d from possible K2 data
                } else {
                    d = ShortArray((length - 8) / 2)
                    for (i in d!!.indices) d!![i] = BCDToDec(
                        data[16 + 2 * i],
                        data[17 + 2 * i]
                    ) //assemble d from 2 bytes data BCD into Dec
                }
            }
        } else d = null
    }

    /**
     * method to create byteArray from Object to send towards IBH_Link
     *
     * @return data
     */
    fun toByteArray(): ByteArray {
        if ((d != null) and (data_type.toInt() == 6)) for (i in d!!.indices) {
            d!![i] = DecToBCD(d!![i]) //assemble d from Dec to BCD
        }
        val adr1 = (data_adr / 0x100).toByte() //disassemble data_adr in d
        val adr2 = (data_adr % 0x100).toByte() //disassemble data_adr in d
        val data: ByteArray
        if (d == null) data =
            ByteArray(16) //if there is no d to send create new array with a length of header + additional header
        else {
            data =
                ByteArray(16 + if (data_type.toInt() == 5) d!!.size else d!!.size * 2) //if there is d to send create new array with a length of header + additional header + (if not timer or counter length of d, else length of d * 2)
            for (i in d!!.indices) {
                if (data_type.toInt() == 5) data[16 + i] = d!![i].toByte() else {
                    data[16 + 2 * i] = (d!![i] / 0x100).toByte() //disasseble d into 2 bytes
                    data[17 + 2 * i] = (d!![i] % 0x100).toByte() //disasseble d into 2 bytes
                }
            }
        }
        data[0] = rx
        data[1] = tx
        data[2] = ln
        data[3] = nr
        data[4] = a
        data[5] = f
        data[6] = b
        data[7] = e
        data[8] = device_adr
        data[9] = data_area
        data[11] = adr1
        data[10] = adr2
        data[12] = data_idx
        data[13] = data_cnt
        data[14] = data_type
        data[15] = function
        return data
    }

    /**
     * method to change BCD into Dec
     *
     * @param thousandHundred
     * @param tennerOne
     * @return decimal
     */
    private fun BCDToDec(thousandHundred: Byte, tennerOne: Byte): Short {
        val intThousandHundred =
            if (thousandHundred < 0) 0x100 + thousandHundred else thousandHundred.toInt()
        val intTennerOne = if (tennerOne < 0) 0x100 + tennerOne else tennerOne.toInt()
        val decimal =
            (intThousandHundred and 0xF0) / 16 * 1000 + (intThousandHundred and 0xF) * 100 + (intTennerOne and 0xF0) / 16 * 10 + (intTennerOne and 0xF)
        return decimal.toShort()
    }

    /**
     * method to change Dec into BCD
     *
     * @param decimal
     * @return bcd
     */
    private fun DecToBCD(decimal: Short): Short {
        val dec = if (decimal < 0) 512 - decimal else decimal.toInt()
        val thousand = dec / 1000
        val hundred = dec % 1000 / 100
        val tenner = dec % 100 / 10
        val one = dec % 10
        val bcd = thousand * 0x10000 + hundred * 0x100 + tenner * 16 + one
        return bcd.toShort()
    }

    companion object {
        /*
	 * setup constants
	 */
        const val NOT_CONN: Byte = 1
        const val CONN_FAILED: Byte = 2
        const val RW_FAILED: Byte = 3
        const val IBHLINK_PORT = 1099
        const val IBHLINK_READ_MAX: Short = 222
        const val IBHLINK_WRITE_MAX: Short = 216
        const val MPI_TASK: Byte = 3
        const val HOST = 255.toByte()
        const val MSG_SIZE: Byte = 8
        const val MSG_PARAM_LEN: Byte = 8
        const val INPUT_AREA: Byte = 0
        const val OUTPUT_AREA: Byte = 1
        const val MPI_READ_WRITE_DB: Byte = 0x31
        const val MPI_GET_OP_STATUS: Byte = 0x32
        const val MPI_READ_WRITE_M: Byte = 0x33
        const val MPI_READ_WRITE_IO: Byte = 0x34
        const val MPI_READ_WRITE_CNT: Byte = 0x35
        const val MPI_READ_WRITE_TIM: Byte = 0x36
        const val MPI_DISCONNECT: Byte = 0x3F
        const val TASK_TDT_UINT8: Byte = 5
        const val TASK_TDT_UINT16: Byte = 6
        const val TASK_TFC_READ: Byte = 1
        const val TASK_TFC_WRITE: Byte = 2
        const val CON_OK: Byte = 0 // service could be executed without an error
        const val CON_UE: Byte =
            1 // timeout from remote station remote station remote station has not responded within 1 sec.timeout
        const val CON_RR: Byte =
            2 // resource unavailable remote station remote station has no left buffer space for the requested service
        const val CON_RS: Byte =
            3 // requested function of master is not activated within the remote station. remote station the connection seems to be closed in the remote station.try to send command again.
        const val CON_NA: Byte =
            17 // no response of the remote station remote station check network wiring, check remote address, check baud rate
        const val CON_DS: Byte =
            18 // master not into the logical token ring network in general check master DP-Address or highest-station-Addres s of other masters. Examine bus wiring to bus short circuits.
        const val CON_LR: Byte =
            20 // Resource of the local FDL controller not available or not sifficient. HOST too many messages. no more segments in DEVICE free
        const val CON_IV: Byte =
            21 // the specified msg.data_cnt parameter invalid HOST check the limit of 222 bytes (read) respectively 216 bytes (write) in msg.data_cnt
        const val CON_TO: Byte =
            48 // timeout, the request message was accepted but no indication is sent back by the remote station remote station MPI protocol error, or station not presentor
        const val CON_SE: Byte =
            57 // Sequence fault, internal state machine error. Remote station does not react like awaited or a reconnection was retried while connection is already open or device has no SAPs left to open connection channel
        const val REJ_IV =
            0x85.toByte() // specified offset address out of limits or not known in the remote station HOST please check msg.data_adr if present or offset parameter in request message
        const val REJ_PDU =
            0x86.toByte() // wrong PDU coding in the MPI response of the remote station DEVICE contact hotline
        const val REJ_OP =
            0x87.toByte() // specified length to write or to read results in an access outside the limits HOST please check msg.data_cnt length in request message
    }
}