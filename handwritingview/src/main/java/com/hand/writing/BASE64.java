package com.hand.writing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * BASE64编解码帮助类
 */
public class BASE64 {
    //------------------------------------常量
    /**
     * base64加密字符--A~Z,a~z,0~9,+,/
     */
    private static char[] base64EncodeChars = new char[]{'A', 'B', 'C', 'D',
            'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q',
            'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
            'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
            'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3',
            '4', '5', '6', '7', '8', '9', '+', '/'};
    private static byte[] base64DecodeChars = new byte[]{-1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59,
            60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
            10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1,
            -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37,
            38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1,
            -1, -1};

    //-------------------------------公用方法

    /**
     * 用Base64编码 byte[]->String
     *
     * @param data 待编码的字符串
     * @return 编码后的字符串
     */
    public static String encode(byte[] data) {
        //----------准备工作
        StringBuffer sb = new StringBuffer();
        int len = data.length;
        int i = 0;
        int b1, b2, b3;
        //------------开始工作
        while (i < len) {
            b1 = data[i++] & 0xff;
            if (i == len) {
                //到达最后第二个，数据对3取余1
                sb.append(base64EncodeChars[b1 >>> 2]);
                sb.append(base64EncodeChars[(b1 & 0x3) << 4]);
                sb.append("==");
                break;
            }
            b2 = data[i++] & 0xff;
            if (i == len) {
                //到达最后第三个，数据对3取余2
                sb.append(base64EncodeChars[b1 >>> 2]);
                sb.append(base64EncodeChars[((b1 & 0x03) << 4)
                        | ((b2 & 0xf0) >>> 4)]);
                sb.append(base64EncodeChars[(b2 & 0x0f) << 2]);
                sb.append("=");
                break;
            }
            b3 = data[i++] & 0xff;
            sb.append(base64EncodeChars[b1 >>> 2]);//第一个字符的二进制值取前6位，前面补上两个0
            sb.append(base64EncodeChars[((b1 & 0x03) << 4)
                    | ((b2 & 0xf0) >>> 4)]);//字符1取后2位，放于3、4位置，字符2取前4位，前面补上两个0
            sb.append(base64EncodeChars[((b2 & 0x0f) << 2)
                    | ((b3 & 0xc0) >>> 6)]);//字符2取后4位，字符3取前两位，补上2个0
            sb.append(base64EncodeChars[b3 & 0x3f]);//字符3取后6位，补上0
        }
        return sb.toString();
    }

    /**
     * 对base64编码的字符串进行解码，String->byte[]
     *
     * @param str 待解码的字符串
     * @return 解码后的字符串
     * @throws UnsupportedEncodingException
     */
    public static byte[] decode(String str) throws UnsupportedEncodingException {
        //---------------准备工作
        StringBuffer sb = new StringBuffer();
        byte[] data = str.getBytes("US-ASCII");
        int len = data.length;
        int i = 0;
        int b1, b2, b3, b4;
        //-----------开始工作
        while (i < len) {
            do {
                b1 = base64DecodeChars[data[i++]];
            } while (i < len && b1 == -1);
            if (b1 == -1)
                break;

            do {
                b2 = base64DecodeChars[data[i++]];
            } while (i < len && b2 == -1);
            if (b2 == -1)
                break;
            sb.append((char) ((b1 << 2) | ((b2 & 0x30) >>> 4)));//b1取后面6个，b2取3、4

            do {
                b3 = data[i++];
                if (b3 == 61)
                    return sb.toString().getBytes("iso8859-1");
                b3 = base64DecodeChars[b3];
            } while (i < len && b3 == -1);
            if (b3 == -1)
                break;
            sb.append((char) (((b2 & 0x0f) << 4) | ((b3 & 0x3c) >>> 2)));//b2取后面4个，b3取中间4个

            do {
                b4 = data[i++];
                if (b4 == 61)
                    return sb.toString().getBytes("iso8859-1");
                b4 = base64DecodeChars[b4];
            } while (i < len && b4 == -1);
            if (b4 == -1)
                break;
            sb.append((char) (((b3 & 0x03) << 6) | b4));
        }
        data = null;
        return sb.toString().getBytes("iso8859-1");
    }

    /**
     * <p>
     * 读取文件，用base64编码 ,输出字符串
     * </p>
     *
     * @param filePath 文件路径
     * @return 输出编码后的字符串
     * @throws Exception
     */
    public static String encodeFromFile(String filePath) throws Exception {
        File file = new File(filePath);
        FileInputStream inputFile = new FileInputStream(file);
        byte[] buffer = new byte[(int) file.length()];
        inputFile.read(buffer);
        inputFile.close();
        return encode(buffer);
    }

    /**
     * <p>
     * 读取文件，对内容解码成 字符串
     * </p>
     *
     * @param filePath 文件路径
     * @return 解码后的字符串
     * @throws Exception
     */
    public static byte[] decodeFromFile(String filePath) throws Exception {
        File file = new File(filePath);
        FileInputStream inputFile = new FileInputStream(file);
        byte[] buffer = new byte[(int) file.length()];
        inputFile.read(buffer);
        inputFile.close();
        return decode(new String(buffer));
    }

    /**
     * <p>
     * 将base64字符解码保存文件
     * </p>
     *
     * @param base64Code 待编码的字符串
     * @param targetPath 目标存储的文件路径
     * @throws Exception
     */
    public static void decodeToFile(String base64Code, String targetPath)
            throws Exception {
        byte[] buffer = decode((base64Code));
        FileOutputStream out = new FileOutputStream(targetPath);
        out.write(buffer);
        out.close();
    }
}
