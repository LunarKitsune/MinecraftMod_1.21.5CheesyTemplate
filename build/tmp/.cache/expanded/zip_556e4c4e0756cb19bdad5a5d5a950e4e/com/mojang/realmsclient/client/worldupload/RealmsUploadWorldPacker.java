package com.mojang.realmsclient.client.worldupload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.zip.GZIPOutputStream;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

@OnlyIn(Dist.CLIENT)
public class RealmsUploadWorldPacker {
    private static final long SIZE_LIMIT = 5368709120L;
    private static final String WORLD_FOLDER_NAME = "world";
    private final BooleanSupplier isCanceled;
    private final Path directoryToPack;

    public static File pack(Path pDirectoryToPack, BooleanSupplier pIsCanceled) throws IOException {
        return new RealmsUploadWorldPacker(pDirectoryToPack, pIsCanceled).tarGzipArchive();
    }

    private RealmsUploadWorldPacker(Path pDirectoryToPack, BooleanSupplier pIsCanceled) {
        this.isCanceled = pIsCanceled;
        this.directoryToPack = pDirectoryToPack;
    }

    private File tarGzipArchive() throws IOException {
        TarArchiveOutputStream tararchiveoutputstream = null;

        File file2;
        try {
            File file1 = File.createTempFile("realms-upload-file", ".tar.gz");
            tararchiveoutputstream = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(file1)));
            tararchiveoutputstream.setLongFileMode(3);
            this.addFileToTarGz(tararchiveoutputstream, this.directoryToPack, "world", true);
            if (this.isCanceled.getAsBoolean()) {
                throw new RealmsUploadCanceledException();
            }

            tararchiveoutputstream.finish();
            this.verifyBelowSizeLimit(file1.length());
            file2 = file1;
        } finally {
            if (tararchiveoutputstream != null) {
                tararchiveoutputstream.close();
            }
        }

        return file2;
    }

    private void addFileToTarGz(TarArchiveOutputStream pStream, Path pDirectory, String pPrefix, boolean pIsRootDirectory) throws IOException {
        if (this.isCanceled.getAsBoolean()) {
            throw new RealmsUploadCanceledException();
        } else {
            this.verifyBelowSizeLimit(pStream.getBytesWritten());
            File file1 = pDirectory.toFile();
            String s = pIsRootDirectory ? pPrefix : pPrefix + file1.getName();
            TarArchiveEntry tararchiveentry = new TarArchiveEntry(file1, s);
            pStream.putArchiveEntry(tararchiveentry);
            if (file1.isFile()) {
                try (InputStream inputstream = new FileInputStream(file1)) {
                    inputstream.transferTo(pStream);
                }

                pStream.closeArchiveEntry();
            } else {
                pStream.closeArchiveEntry();
                File[] afile = file1.listFiles();
                if (afile != null) {
                    for (File file2 : afile) {
                        this.addFileToTarGz(pStream, file2.toPath(), s + "/", false);
                    }
                }
            }
        }
    }

    private void verifyBelowSizeLimit(long pSize) {
        if (pSize > 5368709120L) {
            throw new RealmsUploadTooLargeException(5368709120L);
        }
    }
}