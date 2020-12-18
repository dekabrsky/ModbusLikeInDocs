package com.example.modbuslikeindocs;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.ReadInputDiscretesRequest;
import net.wimpi.modbus.msg.ReadInputDiscretesResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.net.TCPMasterConnection;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(new Task()).start();
    }

    public  class Task extends Thread {
        public void run() {
            /* The important instances of the classes mentioned before */
            TCPMasterConnection con = null; //the connection
            ModbusTCPTransaction trans = null; //the transaction
            ReadMultipleRegistersRequest req = null; //the request
            ReadMultipleRegistersResponse res = null; //the response

            /* Variables for storing the parameters */
            InetAddress addr = null; //the slave's address
            int port = 50000;
            int ref = 0; //the reference; offset where to start reading from
            int count = 0; //the number of DI's to read
            int repeat = 1; //a loop for repeating the transaction

            //1. Setup the parameters
            try {
                addr = InetAddress.getByName("192.168.0.71");
                Log.d("Mb","Address yes");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            ref = 0x0708;
            count = 1;
            repeat = 5;

            try {
                // Open the connection
                con = new TCPMasterConnection(addr);
                con.setPort(port);
                con.connect();
                Log.d("MODBUS", "connection normalno");
            } catch (Exception e) {
                Log.d("MODBUS", "connection error", e);
            }

            //3. Prepare the request
            req = new ReadMultipleRegistersRequest(ref, count);
            req.setUnitID(1);
            req.setHeadless();
            String crc = getCRC(req.getHexMessage());
            System.out.println("Запрос=" + req.getHexMessage());
            //4. Prepare the transaction
            trans = new ModbusTCPTransaction(con);
            trans.setRequest(req);

            //5. Execute the transaction repeat times
            int k = 0;
            do {
                try {
                    trans.execute();
                    Log.d("Mb","Transaction yes");
                } catch (ModbusException e) {
                    e.printStackTrace();
                }
                res = (ReadMultipleRegistersResponse) trans.getResponse();
                if (res!=null)
                    System.out.println("Ответ=" + res.getHexMessage());
                k++;
            } while (k < repeat);



            //6. Close the connection
            con.close();
        }
    }

    public static String getCRC(String data) {
        data = data.replace(" ", "");
        int len = data.length();
        if (!(len % 2 == 0)) {
            return "0000";
        }
        int num = len / 2;
        byte[] para = new byte[num];
        for (int i = 0; i < num; i++) {
            int value = Integer.valueOf(data.substring(i * 2, 2 * (i + 1)), 16);
            para[i] = (byte) value;
        }
        return getCRC(para);
    }

    /**
     *Calculate CRC16 check code
     *
     * @param bytes
     *Byte array
     *@ return {@ link string} check code
 * @since 1.0
     */
    public static String getCRC(byte[] bytes) {
        //CRC registers are all 1
        int CRC = 0x0000ffff;
        //Polynomial check value
        int POLYNOMIAL = 0x0000a001;
        int i, j;
        for (i = 0; i < bytes.length; i++) {
            CRC ^= ((int) bytes[i] & 0x000000ff);
            for (j = 0; j < 8; j++) {
                if ((CRC & 0x00000001) != 0) {
                    CRC >>= 1;
                    CRC ^= POLYNOMIAL;
                } else {
                    CRC >>= 1;
                }
            }
        }
        //Result converted to hex
        String result = Integer.toHexString(CRC).toUpperCase();
        if (result.length() != 4) {
            StringBuffer sb = new StringBuffer("0000");
            result = sb.replace(4 - result.length(), 4, result).toString();
        }
        //High position in the front position in the back
        //return result.substring(2, 4) + " " + result.substring(0, 2);
        //Exchange high low, low in front, high in back
        return result.substring(2, 4) + " " + result.substring(0, 2);
    }
}