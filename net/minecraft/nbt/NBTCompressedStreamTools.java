package net.minecraft.nbt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;

public class NBTCompressedStreamTools {
    public static NBTTagCompound readCompressed(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);

        NBTTagCompound var2;
        try {
            var2 = readCompressed(inputStream);
        } catch (Throwable var5) {
            try {
                inputStream.close();
            } catch (Throwable var4) {
                var5.addSuppressed(var4);
            }

            throw var5;
        }

        inputStream.close();
        return var2;
    }

    public static NBTTagCompound readCompressed(InputStream stream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(stream)));

        NBTTagCompound var2;
        try {
            var2 = read(dataInputStream, NBTReadLimiter.UNLIMITED);
        } catch (Throwable var5) {
            try {
                dataInputStream.close();
            } catch (Throwable var4) {
                var5.addSuppressed(var4);
            }

            throw var5;
        }

        dataInputStream.close();
        return var2;
    }

    public static void writeCompressed(NBTTagCompound compound, File file) throws IOException {
        OutputStream outputStream = new FileOutputStream(file);

        try {
            writeCompressed(compound, outputStream);
        } catch (Throwable var6) {
            try {
                outputStream.close();
            } catch (Throwable var5) {
                var6.addSuppressed(var5);
            }

            throw var6;
        }

        outputStream.close();
    }

    public static void writeCompressed(NBTTagCompound compound, OutputStream stream) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(stream)));

        try {
            write(compound, dataOutputStream);
        } catch (Throwable var6) {
            try {
                dataOutputStream.close();
            } catch (Throwable var5) {
                var6.addSuppressed(var5);
            }

            throw var6;
        }

        dataOutputStream.close();
    }

    public static void write(NBTTagCompound compound, File file) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);

        try {
            DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);

            try {
                write(compound, dataOutputStream);
            } catch (Throwable var8) {
                try {
                    dataOutputStream.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }

                throw var8;
            }

            dataOutputStream.close();
        } catch (Throwable var9) {
            try {
                fileOutputStream.close();
            } catch (Throwable var6) {
                var9.addSuppressed(var6);
            }

            throw var9;
        }

        fileOutputStream.close();
    }

    @Nullable
    public static NBTTagCompound read(File file) throws IOException {
        if (!file.exists()) {
            return null;
        } else {
            FileInputStream fileInputStream = new FileInputStream(file);

            NBTTagCompound var3;
            try {
                DataInputStream dataInputStream = new DataInputStream(fileInputStream);

                try {
                    var3 = read(dataInputStream, NBTReadLimiter.UNLIMITED);
                } catch (Throwable var7) {
                    try {
                        dataInputStream.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }

                    throw var7;
                }

                dataInputStream.close();
            } catch (Throwable var8) {
                try {
                    fileInputStream.close();
                } catch (Throwable var5) {
                    var8.addSuppressed(var5);
                }

                throw var8;
            }

            fileInputStream.close();
            return var3;
        }
    }

    public static NBTTagCompound read(DataInput input) throws IOException {
        return read(input, NBTReadLimiter.UNLIMITED);
    }

    public static NBTTagCompound read(DataInput input, NBTReadLimiter tracker) throws IOException {
        NBTBase tag = readUnnamedTag(input, 0, tracker);
        if (tag instanceof NBTTagCompound) {
            return (NBTTagCompound)tag;
        } else {
            throw new IOException("Root tag must be a named compound tag");
        }
    }

    public static void write(NBTTagCompound compound, DataOutput output) throws IOException {
        writeUnnamedTag(compound, output);
    }

    private static void writeUnnamedTag(NBTBase element, DataOutput output) throws IOException {
        output.writeByte(element.getTypeId());
        if (element.getTypeId() != 0) {
            output.writeUTF("");
            element.write(output);
        }
    }

    private static NBTBase readUnnamedTag(DataInput input, int depth, NBTReadLimiter tracker) throws IOException {
        byte b = input.readByte();
        if (b == 0) {
            return NBTTagEnd.INSTANCE;
        } else {
            input.readUTF();

            try {
                return NBTTagTypes.getType(b).load(input, depth, tracker);
            } catch (IOException var7) {
                CrashReport crashReport = CrashReport.forThrowable(var7, "Loading NBT data");
                CrashReportSystemDetails crashReportCategory = crashReport.addCategory("NBT Tag");
                crashReportCategory.setDetail("Tag type", b);
                throw new ReportedException(crashReport);
            }
        }
    }
}
