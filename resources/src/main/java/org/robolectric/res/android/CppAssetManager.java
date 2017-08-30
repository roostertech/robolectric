package org.robolectric.res.android;

import static org.robolectric.res.android.Util.ALOGV;
import static org.robolectric.res.android.Util.ALOGW;
import static org.robolectric.res.android.Util.ATRACE_CALL;
import static org.robolectric.res.android.Util.LOG_FATAL_IF;
import static org.robolectric.res.android.Util.isTruthy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;
import org.robolectric.res.android.Asset.AccessMode;
import org.robolectric.res.android.CppAssetManager.FileType;

// transliterated from https://android.googlesource.com/platform/frameworks/base/+/android-7.1.1_r13/libs/androidfw/AssetManager.cpp
public class CppAssetManager {

  enum FileType {
    kFileTypeUnknown,
    kFileTypeNonexistent,       // i.e. ENOENT
    kFileTypeRegular,
    kFileTypeDirectory,
    kFileTypeCharDev,
    kFileTypeBlockDev,
    kFileTypeFifo,
    kFileTypeSymlink,
    kFileTypeSocket,
  }


  // transliterated from https://cs.corp.google.com/android/frameworks/base/libs/androidfw/include/androidfw/AssetManager.h
  private static class asset_path
  {
//    asset_path() : path(""), type(kFileTypeRegular), idmap(""),
//      isSystemOverlay(false), isSystemAsset(false) {}


    public asset_path() {
      this(new String8(), FileType.kFileTypeRegular, "", false, false);
    }

    public asset_path(String8 path, FileType fileType, String idmap,
        boolean isSystemOverlay,
        boolean isSystemAsset) {
      this.path = path;
      this.type = fileType;
      this.idmap = idmap;
      this.isSystemOverlay = isSystemOverlay;
      this.isSystemAsset = isSystemAsset;
    }

    String8 path;
    FileType type;
    String idmap;
    boolean isSystemOverlay;
    boolean isSystemAsset;
  }

  private final Object   mLock = new Object();

  //ZipSet          mZipSet;
  Object mZipSet;

  private final List<asset_path> mAssetPaths = new ArrayList<>();
  private String mLocale;

  private ResTable mResources;
  private ResTableConfig mConfig;


//  static final boolean kIsDebug = false;
//  
  static final String kAssetsRoot = "assets";
  static final String kAppZipName = null; //"classes.jar";
//  static final char* kSystemAssets = "framework/framework-res.apk";
//  static final char* kResourceCache = "resource-cache";
//  
//  static final char* kExcludeExtension = ".EXCLUDE";
//  

  // TODO: figure out how to translate this to Java
  // static Asset final kExcludedAsset = (Asset*) 0xd000000d;
  static final Asset kExcludedAsset = null;
//  
//  static volatile int gCount = 0;
//  
//  final char* RESOURCES_FILENAME = "resources.arsc";
//  final char* IDMAP_BIN = "/system/bin/idmap";
//  final char* OVERLAY_DIR = "/vendor/overlay";
//  final char* OVERLAY_THEME_DIR_PROPERTY = "ro.boot.vendor.overlay.theme";
//  final char* TARGET_PACKAGE_NAME = "android";
//  final char* TARGET_APK_PATH = "/system/framework/framework-res.apk";
//  final char* IDMAP_DIR = "/data/resource-cache";
//  
//  namespace {
//  
//  String8 idmapPathForPackagePath(final String8& pkgPath) {
//      final char* root = getenv("ANDROID_DATA");
//      LOG_ALWAYS_FATAL_IF(root == NULL, "ANDROID_DATA not set");
//      String8 path(root);
//      path.appendPath(kResourceCache);
//  
//      char buf[256]; // 256 chars should be enough for anyone...
//      strncpy(buf, pkgPath.string(), 255);
//      buf[255] = '\0';
//      char* filename = buf;
//      while (*filename && *filename == '/') {
//          ++filename;
//      }
//      char* p = filename;
//      while (*p) {
//          if (*p == '/') {
//              *p = '@';
//          }
//          ++p;
//      }
//      path.appendPath(filename);
//      path.append("@idmap");
//  
//      return path;
//  }
//  
//  /*
//   * Like strdup(), but uses C++ "new" operator instead of malloc.
//   */
//  static char* strdupNew(final char* str) {
//      char* newStr;
//      int len;
//  
//      if (str == NULL)
//          return NULL;
//  
//      len = strlen(str);
//      newStr = new char[len+1];
//      memcpy(newStr, str, len+1);
//  
//      return newStr;
//  }
//  
//  } // namespace
//  
//  /*
//   * ===========================================================================
//   *      AssetManager
//   * ===========================================================================
//   */
//  
//  int getGlobalCount() {
//      return gCount;
//  }
//  
//  AssetManager() :
//          mLocale(NULL), mResources(NULL), mConfig(new ResTable_config) {
//      int count = android_atomic_inc(&gCount) + 1;
//      if (kIsDebug) {
//          ALOGI("Creating AssetManager %p #%d\n", this, count);
//      }
//      memset(mConfig, 0, sizeof(ResTable_config));
//  }
//  
//  ~AssetManager() {
//      int count = android_atomic_dec(&gCount);
//      if (kIsDebug) {
//          ALOGI("Destroying AssetManager in %p #%d\n", this, count);
//      }
//  
//      delete mConfig;
//      delete mResources;
//  
//      // don't have a String class yet, so make sure we clean up
//      delete[] mLocale;
//  }
//  
  boolean addAssetPath(
          final String8 path, @Nullable  Ref<Integer> cookie, boolean appAsLib, boolean isSystemAsset) {
      synchronized (mLock) {

        asset_path ap = new asset_path();

        String8 realPath = path;
        if (kAppZipName != null) {
          realPath.appendPath(kAppZipName);
        }
        ap.type = getFileType(realPath.string());
        if (ap.type == FileType.kFileTypeRegular) {
          ap.path = realPath;
        } else {
          ap.path = path;
          ap.type = getFileType(path.string());
          if (ap.type != FileType.kFileTypeDirectory && ap.type != FileType.kFileTypeRegular) {
            ALOGW("Asset path %s is neither a directory nor file (type=%s).",
                path.toString(), ap.type.name());
            return false;
          }
        }

        // Skip if we have it already.
        for (int i = 0; i < mAssetPaths.size(); i++) {
          if (mAssetPaths.get(i).path.equals(ap.path)) {
            if (cookie != null) {
                  cookie.set(i + 1);
            }
            return true;
          }
        }

        ALOGV("In %p Asset %s path: %s", this,
            ap.type == FileType.kFileTypeDirectory ? "dir" : "zip", ap.path.toString());

        ap.isSystemAsset = isSystemAsset;
        mAssetPaths.add(ap);

        // new paths are always added at the end
        if (cookie != null) {
          cookie.set(mAssetPaths.size());
        }

        // TODO: implement this?
  //#ifdef __ANDROID__
        // Load overlays, if any
        //asset_path oap;
        //for (int idx = 0; mZipSet.getOverlay(ap.path, idx, & oap)
        //  ; idx++){
        //  oap.isSystemAsset = isSystemAsset;
        //  mAssetPaths.add(oap);
       // }
  //#endif

        if (mResources != null) {
          appendPathToResTable(ap, appAsLib);
        }

        return true;
      }
  }
//  
//  boolean addOverlayPath(final String8& packagePath, int* cookie)
//  {
//      final String8 idmapPath = idmapPathForPackagePath(packagePath);
//  
//      AutoMutex _l(mLock);
//  
//      for (int i = 0; i < mAssetPaths.size(); ++i) {
//          if (mAssetPaths[i].idmap == idmapPath) {
//             *cookie = static_cast<int>(i + 1);
//              return true;
//           }
//       }
//  
//      Asset* idmap = NULL;
//      if ((idmap = openAssetFromFileLocked(idmapPath, Asset.ACCESS_BUFFER)) == NULL) {
//          ALOGW("failed to open idmap file %s\n", idmapPath.string());
//          return false;
//      }
//  
//      String8 targetPath;
//      String8 overlayPath;
//      if (!ResTable.getIdmapInfo(idmap.getBuffer(false), idmap.getLength(),
//                  NULL, NULL, NULL, &targetPath, &overlayPath)) {
//          ALOGW("failed to read idmap file %s\n", idmapPath.string());
//          delete idmap;
//          return false;
//      }
//      delete idmap;
//  
//      if (overlayPath != packagePath) {
//          ALOGW("idmap file %s inconcistent: expected path %s does not match actual path %s\n",
//                  idmapPath.string(), packagePath.string(), overlayPath.string());
//          return false;
//      }
//      if (access(targetPath.string(), R_OK) != 0) {
//          ALOGW("failed to access file %s: %s\n", targetPath.string(), strerror(errno));
//          return false;
//      }
//      if (access(idmapPath.string(), R_OK) != 0) {
//          ALOGW("failed to access file %s: %s\n", idmapPath.string(), strerror(errno));
//          return false;
//      }
//      if (access(overlayPath.string(), R_OK) != 0) {
//          ALOGW("failed to access file %s: %s\n", overlayPath.string(), strerror(errno));
//          return false;
//      }
//  
//      asset_path oap;
//      oap.path = overlayPath;
//      oap.type = .getFileType(overlayPath.string());
//      oap.idmap = idmapPath;
//  #if 0
//      ALOGD("Overlay added: targetPath=%s overlayPath=%s idmapPath=%s\n",
//              targetPath.string(), overlayPath.string(), idmapPath.string());
//  #endif
//      mAssetPaths.add(oap);
//      *cookie = static_cast<int>(mAssetPaths.size());
//  
//      if (mResources != NULL) {
//          appendPathToResTable(oap);
//      }
//  
//      return true;
//   }
//  
//  boolean createIdmap(final char* targetApkPath, final char* overlayApkPath,
//          uint32_t targetCrc, uint32_t overlayCrc, uint32_t** outData, int* outSize)
//  {
//      AutoMutex _l(mLock);
//      final String8 paths[2] = { String8(targetApkPath), String8(overlayApkPath) };
//      Asset* assets[2] = {NULL, NULL};
//      boolean ret = false;
//      {
//          ResTable tables[2];
//  
//          for (int i = 0; i < 2; ++i) {
//              asset_path ap;
//              ap.type = kFileTypeRegular;
//              ap.path = paths[i];
//              assets[i] = openNonAssetInPathLocked("resources.arsc",
//                      Asset.ACCESS_BUFFER, ap);
//              if (assets[i] == NULL) {
//                  ALOGW("failed to find resources.arsc in %s\n", ap.path.string());
//                  goto exit;
//              }
//              if (tables[i].add(assets[i]) != NO_ERROR) {
//                  ALOGW("failed to add %s to resource table", paths[i].string());
//                  goto exit;
//              }
//          }
//          ret = tables[0].createIdmap(tables[1], targetCrc, overlayCrc,
//                  targetApkPath, overlayApkPath, (void**)outData, outSize) == NO_ERROR;
//      }
//  
//  exit:
//      delete assets[0];
//      delete assets[1];
//      return ret;
//  }
//  
//  boolean addDefaultAssets()
//  {
//      final char* root = getenv("ANDROID_ROOT");
//      LOG_ALWAYS_FATAL_IF(root == NULL, "ANDROID_ROOT not set");
//  
//      String8 path(root);
//      path.appendPath(kSystemAssets);
//  
//      return addAssetPath(path, NULL, false /* appAsLib */, true /* isSystemAsset */);
//  }
//  
//  int nextAssetPath(final int cookie) final
//  {
//      AutoMutex _l(mLock);
//      final int next = static_cast<int>(cookie) + 1;
//      return next > mAssetPaths.size() ? -1 : next;
//  }
//  
//  String8 getAssetPath(final int cookie) final
//  {
//      AutoMutex _l(mLock);
//      final int which = static_cast<int>(cookie) - 1;
//      if (which < mAssetPaths.size()) {
//          return mAssetPaths[which].path;
//      }
//      return String8();
//  }
//  
//  void setLocaleLocked(final char* locale)
//  {
//      if (mLocale != NULL) {
//          delete[] mLocale;
//      }
//  
//      mLocale = strdupNew(locale);
//      updateResourceParamsLocked();
//  }
//  
//  void setConfiguration(final ResTable_config& config, final char* locale)
//  {
//      AutoMutex _l(mLock);
//      *mConfig = config;
//      if (locale) {
//          setLocaleLocked(locale);
//      } else if (config.language[0] != 0) {
//          char spec[RESTABLE_MAX_LOCALE_LEN];
//          config.getBcp47Locale(spec);
//          setLocaleLocked(spec);
//      } else {
//          updateResourceParamsLocked();
//      }
//  }
//  
//  void getConfiguration(ResTable_config* outConfig) final
//  {
//      AutoMutex _l(mLock);
//      *outConfig = *mConfig;
//  }
//  
  /*
   * Open an asset.
   *
   * The data could be in any asset path. Each asset path could be:
   *  - A directory on disk.
   *  - A Zip archive, uncompressed or compressed.
   *
   * If the file is in a directory, it could have a .gz suffix, meaning it is compressed.
   *
   * We should probably reject requests for "illegal" filenames, e.g. those
   * with illegal characters or "../" backward relative paths.
   */
  Asset open(final String fileName, AccessMode mode)
  {
      synchronized (mLock) {

        LOG_FATAL_IF(mAssetPaths.size() == 0, "No assets added to AssetManager");

        String8 assetName = new String8(kAssetsRoot);
        assetName.appendPath(fileName);
      /*
       * For each top-level asset path, search for the asset.
       */
        int i = mAssetPaths.size();
        while (i > 0) {
          i--;
          ALOGV("Looking for asset '%s' in '%s'\n",
              assetName.string(), mAssetPaths.get(i).path.string());
          Asset pAsset = openNonAssetInPathLocked(assetName.string(), mode,
              mAssetPaths.get(i));
          if (pAsset != null) {
            return Objects.equals(pAsset, kExcludedAsset) ? null  : pAsset;
          }
        }

        return null;
      }
  }
//  
//  /*
//   * Open a non-asset file as if it were an asset.
//   *
//   * The "fileName" is the partial path starting from the application name.
//   */
//  Asset* openNonAsset(final char* fileName, AccessMode mode, int* outCookie)
//  {
//      AutoMutex _l(mLock);
//  
//      LOG_FATAL_IF(mAssetPaths.size() == 0, "No assets added to AssetManager");
//  
//      /*
//       * For each top-level asset path, search for the asset.
//       */
//  
//      int i = mAssetPaths.size();
//      while (i > 0) {
//          i--;
//          ALOGV("Looking for non-asset '%s' in '%s'\n", fileName, mAssetPaths.itemAt(i).path.string());
//          Asset* pAsset = openNonAssetInPathLocked(
//              fileName, mode, mAssetPaths.itemAt(i));
//          if (pAsset != NULL) {
//              if (outCookie != NULL) *outCookie = static_cast<int>(i + 1);
//              return pAsset != kExcludedAsset ? pAsset : NULL;
//          }
//      }
//  
//      return NULL;
//  }
//  
//  Asset* openNonAsset(final int cookie, final char* fileName, AccessMode mode)
//  {
//      final int which = static_cast<int>(cookie) - 1;
//  
//      AutoMutex _l(mLock);
//  
//      LOG_FATAL_IF(mAssetPaths.size() == 0, "No assets added to AssetManager");
//  
//      if (which < mAssetPaths.size()) {
//          ALOGV("Looking for non-asset '%s' in '%s'\n", fileName,
//                  mAssetPaths.itemAt(which).path.string());
//          Asset* pAsset = openNonAssetInPathLocked(
//              fileName, mode, mAssetPaths.itemAt(which));
//          if (pAsset != NULL) {
//              return pAsset != kExcludedAsset ? pAsset : NULL;
//          }
//      }
//  
//      return NULL;
//  }
//  
  /*
   * Get the type of a file in the asset namespace.
   *
   * This currently only works for regular files.  All others (including
   * directories) will return kFileTypeNonexistent.
   */
  FileType getFileType(final String fileName)
  {
      Asset pAsset = null;

      /*
       * Open the asset.  This is less efficient than simply finding the
       * file, but it's not too bad (we don't uncompress or mmap data until
       * the first read() call).
       */
      pAsset = open(fileName, Asset.AccessMode.ACCESS_STREAMING);
      // delete pAsset;

      if (pAsset == null) {
          return FileType.kFileTypeNonexistent;
      } else {
          return FileType.kFileTypeRegular;
      }
  }

  boolean appendPathToResTable(final asset_path ap, boolean appAsLib) {
    URL resource = getClass().getResource("/resources.ap_");
    try {
      ZipFile zipFile = new ZipFile(resource.getFile());
      ZipEntry arscEntry = zipFile.getEntry("resources.arsc");
      InputStream inputStream = zipFile.getInputStream(arscEntry);
      mResources.add(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return true;

    // todo: this   vvv

//      // skip those ap's that correspond to system overlays
//      if (ap.isSystemOverlay) {
//          return true;
//      }
//  
//      Asset* ass = NULL;
//      ResTable* sharedRes = NULL;
//      boolean shared = true;
//      boolean onlyEmptyResources = true;
//      ATRACE_NAME(ap.path.string());
//      Asset* idmap = openIdmapLocked(ap);
//      int nextEntryIdx = mResources.getTableCount();
//      ALOGV("Looking for resource asset in '%s'\n", ap.path.string());
//      if (ap.type != kFileTypeDirectory) {
//          if (nextEntryIdx == 0) {
//              // The first item is typically the framework resources,
//              // which we want to avoid parsing every time.
//              sharedRes = final_cast<AssetManager*>(this).
//                  mZipSet.getZipResourceTable(ap.path);
//              if (sharedRes != NULL) {
//                  // skip ahead the number of system overlay packages preloaded
//                  nextEntryIdx = sharedRes.getTableCount();
//              }
//          }
//          if (sharedRes == NULL) {
//              ass = final_cast<AssetManager*>(this).
//                  mZipSet.getZipResourceTableAsset(ap.path);
//              if (ass == NULL) {
//                  ALOGV("loading resource table %s\n", ap.path.string());
//                  ass = final_cast<AssetManager*>(this).
//                      openNonAssetInPathLocked("resources.arsc",
//                                               Asset.ACCESS_BUFFER,
//                                               ap);
//                  if (ass != NULL && ass != kExcludedAsset) {
//                      ass = final_cast<AssetManager*>(this).
//                          mZipSet.setZipResourceTableAsset(ap.path, ass);
//                  }
//              }
//              
//              if (nextEntryIdx == 0 && ass != NULL) {
//                  // If this is the first resource table in the asset
//                  // manager, then we are going to cache it so that we
//                  // can quickly copy it out for others.
//                  ALOGV("Creating shared resources for %s", ap.path.string());
//                  sharedRes = new ResTable();
//                  sharedRes.add(ass, idmap, nextEntryIdx + 1, false);
//  #ifdef __ANDROID__
//                  final char* data = getenv("ANDROID_DATA");
//                  LOG_ALWAYS_FATAL_IF(data == NULL, "ANDROID_DATA not set");
//                  String8 overlaysListPath(data);
//                  overlaysListPath.appendPath(kResourceCache);
//                  overlaysListPath.appendPath("overlays.list");
//                  addSystemOverlays(overlaysListPath.string(), ap.path, sharedRes, nextEntryIdx);
//  #endif
//                  sharedRes = final_cast<AssetManager*>(this).
//                      mZipSet.setZipResourceTable(ap.path, sharedRes);
//              }
//          }
//      } else {
//          ALOGV("loading resource table %s\n", ap.path.string());
//          ass = final_cast<AssetManager*>(this).
//              openNonAssetInPathLocked("resources.arsc",
//                                       Asset.ACCESS_BUFFER,
//                                       ap);
//          shared = false;
//      }
//  
//      if ((ass != NULL || sharedRes != NULL) && ass != kExcludedAsset) {
//          ALOGV("Installing resource asset %p in to table %p\n", ass, mResources);
//          if (sharedRes != NULL) {
//              ALOGV("Copying existing resources for %s", ap.path.string());
//              mResources.add(sharedRes, ap.isSystemAsset);
//          } else {
//              ALOGV("Parsing resources for %s", ap.path.string());
//              mResources.add(ass, idmap, nextEntryIdx + 1, !shared, appAsLib, ap.isSystemAsset);
//          }
//          onlyEmptyResources = false;
//  
//          if (!shared) {
//              delete ass;
//          }
//      } else {
//          ALOGV("Installing empty resources in to table %p\n", mResources);
//          mResources.addEmpty(nextEntryIdx + 1);
//      }
//  
//      if (idmap != NULL) {
//          delete idmap;
//      }
//      return onlyEmptyResources;
  }

  final ResTable getResTable(boolean required) {
    ResTable rt = mResources;
    if (isTruthy(rt)) {
      return rt;
    }

    // Iterate through all asset packages, collecting resources from each.

    synchronized (mLock) {
      if (mResources != null) {
        return mResources;
      }

      if (required) {
        LOG_FATAL_IF(mAssetPaths.size() == 0, "No assets added to AssetManager");
      }

      mResources = new ResTable();
      updateResourceParamsLocked();

      boolean onlyEmptyResources = true;
      final int N = mAssetPaths.size();
      for (int i=0; i<N; i++) {
        boolean empty = appendPathToResTable(mAssetPaths.get(i), false);
        onlyEmptyResources = onlyEmptyResources && empty;
      }

      if (required && onlyEmptyResources) {
        ALOGW("Unable to find resources file resources.arsc");
//          delete mResources;
        mResources = null;
      }

      return mResources;
    }
  }

  void updateResourceParamsLocked() {
    ATRACE_CALL();
    ResTable res = mResources;
    if (!isTruthy(res)) {
      return;
    }

    // todo: implement
//    if (isTruthy(mLocale)) {
//      mConfig.setBcp47Locale(mLocale);
//    } else {
//      mConfig.clearLocale();
//    }

    res.setParameters(mConfig);
  }

//  Asset* openIdmapLocked(final struct asset_path& ap) final
//  {
//      Asset* ass = NULL;
//      if (ap.idmap.size() != 0) {
//          ass = final_cast<AssetManager*>(this).
//              openAssetFromFileLocked(ap.idmap, Asset.ACCESS_BUFFER);
//          if (ass) {
//              ALOGV("loading idmap %s\n", ap.idmap.string());
//          } else {
//              ALOGW("failed to load idmap %s\n", ap.idmap.string());
//          }
//      }
//      return ass;
//  }
//  
//  void addSystemOverlays(final char* pathOverlaysList,
//          final String8& targetPackagePath, ResTable* sharedRes, int offset) final
//  {
//      FILE* fin = fopen(pathOverlaysList, "r");
//      if (fin == NULL) {
//          return;
//      }
//  
//  #ifndef _WIN32
//      if (TEMP_FAILURE_RETRY(flock(fileno(fin), LOCK_SH)) != 0) {
//          fclose(fin);
//          return;
//      }
//  #endif
//      char buf[1024];
//      while (fgets(buf, sizeof(buf), fin)) {
//          // format of each line:
//          //   <path to apk><space><path to idmap><newline>
//          char* space = strchr(buf, ' ');
//          char* newline = strchr(buf, '\n');
//          asset_path oap;
//  
//          if (space == NULL || newline == NULL || newline < space) {
//              continue;
//          }
//  
//          oap.path = String8(buf, space - buf);
//          oap.type = kFileTypeRegular;
//          oap.idmap = String8(space + 1, newline - space - 1);
//          oap.isSystemOverlay = true;
//  
//          Asset* oass = final_cast<AssetManager*>(this).
//              openNonAssetInPathLocked("resources.arsc",
//                      Asset.ACCESS_BUFFER,
//                      oap);
//  
//          if (oass != NULL) {
//              Asset* oidmap = openIdmapLocked(oap);
//              offset++;
//              sharedRes.add(oass, oidmap, offset + 1, false);
//              final_cast<AssetManager*>(this).mAssetPaths.add(oap);
//              final_cast<AssetManager*>(this).mZipSet.addOverlay(targetPackagePath, oap);
//              delete oidmap;
//          }
//      }
//  
//  #ifndef _WIN32
//      TEMP_FAILURE_RETRY(flock(fileno(fin), LOCK_UN));
//  #endif
//      fclose(fin);
//  }

  public final ResTable getResources() {
    return getResources(true);
  }

  final ResTable getResources(boolean required) {
      final ResTable rt = getResTable(required);
      return rt;
  }

//  boolean isUpToDate()
//  {
//      AutoMutex _l(mLock);
//      return mZipSet.isUpToDate();
//  }
//  
//  void getLocales(Vector<String8>* locales, boolean includeSystemLocales) final
//  {
//      ResTable* res = mResources;
//      if (res != NULL) {
//          res.getLocales(locales, includeSystemLocales, true /* mergeEquivalentLangs */);
//      }
//  }
//  
  /*
   * Open a non-asset file as if it were an asset, searching for it in the
   * specified app.
   *
   * Pass in a NULL values for "appName" if the common app directory should
   * be used.
   */
  Asset openNonAssetInPathLocked(final String fileName, AccessMode mode,
      final asset_path ap)
  {
      Asset pAsset = null;

      /* look at the filesystem on disk */
      if (ap.type == FileType.kFileTypeDirectory) {
          String8 path = new String8(ap.path);
          path.appendPath(fileName);

          pAsset = openAssetFromFileLocked(path, mode);

          if (pAsset == null) {
              /* try again, this time with ".gz" */
              path.append(".gz");
              pAsset = openAssetFromFileLocked(path, mode);
          }

          if (pAsset != null) {
              //printf("FOUND NA '%s' on disk\n", fileName);
              pAsset.setAssetSource(path);
          }

      /* look inside the zip file */
      } else {
//          String8 path = new String8(fileName);
//
//          /* check the appropriate Zip file */
//          ZipFileRO* pZip = getZipFileLocked(ap);
//          if (pZip != NULL) {
//              //printf("GOT zip, checking NA '%s'\n", (final char*) path);
//              ZipEntryRO entry = pZip.findEntryByName(path.string());
//              if (entry != NULL) {
//                  //printf("FOUND NA in Zip file for %s\n", appName ? appName : kAppCommon);
//                  pAsset = openAssetFromZipLocked(pZip, entry, mode, path);
//                  pZip.releaseEntry(entry);
//              }
//          }
//
//          if (pAsset != NULL) {
//              /* create a "source" name, for debug/display */
//              pAsset.setAssetSource(
//                      createZipSourceNameLocked(ZipSet.getPathName(ap.path.string()), String8(""),
//                                                  String8(fileName)));
//          }
      }

      return pAsset;
  }
//  
//  /*
//   * Create a "source name" for a file from a Zip archive.
//   */
//  String8 createZipSourceNameLocked(final String8& zipFileName,
//      final String8& dirName, final String8& fileName)
//  {
//      String8 sourceName("zip:");
//      sourceName.append(zipFileName);
//      sourceName.append(":");
//      if (dirName.length() > 0) {
//          sourceName.appendPath(dirName);
//      }
//      sourceName.appendPath(fileName);
//      return sourceName;
//  }
//  
//  /*
//   * Create a path to a loose asset (asset-base/app/rootDir).
//   */
//  String8 createPathNameLocked(final asset_path& ap, final char* rootDir)
//  {
//      String8 path(ap.path);
//      if (rootDir != NULL) path.appendPath(rootDir);
//      return path;
//  }
//  
//  /*
//   * Return a pointer to one of our open Zip archives.  Returns NULL if no
//   * matching Zip file exists.
//   */
//  ZipFileRO* getZipFileLocked(final asset_path& ap)
//  {
//      ALOGV("getZipFileLocked() in %p\n", this);
//  
//      return mZipSet.getZip(ap.path);
//  }
//  
  /*
   * Try to open an asset from a file on disk.
   *
   * If the file is compressed with gzip, we seek to the start of the
   * deflated data and pass that in (just like we would for a Zip archive).
   *
   * For uncompressed data, we may already have an mmap()ed version sitting
   * around.  If so, we want to hand that to the Asset instead.
   *
   * This returns NULL if the file doesn't exist, couldn't be opened, or
   * claims to be a ".gz" but isn't.
   */
  Asset openAssetFromFileLocked(final String8 pathName,
      AccessMode mode)
  {
      Asset pAsset = null;

      if (pathName.getPathExtension().toLowerCase().equals(".gz")) {
          //printf("TRYING '%s'\n", (final char*) pathName);
          pAsset = Asset.createFromCompressedFile(pathName.string(), mode);
      } else {
          //printf("TRYING '%s'\n", (final char*) pathName);
          pAsset = Asset.createFromFile(pathName.string(), mode);
      }

      return pAsset;
  }
//  
//  /*
//   * Given an entry in a Zip archive, create a new Asset object.
//   *
//   * If the entry is uncompressed, we may want to create or share a
//   * slice of shared memory.
//   */
//  Asset* openAssetFromZipLocked(final ZipFileRO* pZipFile,
//      final ZipEntryRO entry, AccessMode mode, final String8& entryName)
//  {
//      Asset* pAsset = NULL;
//  
//      // TODO: look for previously-created shared memory slice?
//      uint16_t method;
//      uint32_t uncompressedLen;
//  
//      //printf("USING Zip '%s'\n", pEntry.getFileName());
//  
//      if (!pZipFile.getEntryInfo(entry, &method, &uncompressedLen, NULL, NULL,
//              NULL, NULL))
//      {
//          ALOGW("getEntryInfo failed\n");
//          return NULL;
//      }
//  
//      FileMap* dataMap = pZipFile.createEntryFileMap(entry);
//      if (dataMap == NULL) {
//          ALOGW("create map from entry failed\n");
//          return NULL;
//      }
//  
//      if (method == ZipFileRO.kCompressStored) {
//          pAsset = Asset.createFromUncompressedMap(dataMap, mode);
//          ALOGV("Opened uncompressed entry %s in zip %s mode %d: %p", entryName.string(),
//                  dataMap.getFileName(), mode, pAsset);
//      } else {
//          pAsset = Asset.createFromCompressedMap(dataMap,
//              static_cast<int>(uncompressedLen), mode);
//          ALOGV("Opened compressed entry %s in zip %s mode %d: %p", entryName.string(),
//                  dataMap.getFileName(), mode, pAsset);
//      }
//      if (pAsset == NULL) {
//          /* unexpected */
//          ALOGW("create from segment failed\n");
//      }
//  
//      return pAsset;
//  }
//  
//  /*
//   * Open a directory in the asset namespace.
//   *
//   * An "asset directory" is simply the combination of all asset paths' "assets/" directories.
//   *
//   * Pass in "" for the root dir.
//   */
//  AssetDir* openDir(final char* dirName)
//  {
//      AutoMutex _l(mLock);
//  
//      AssetDir* pDir = NULL;
//      SortedVector<AssetDir.FileInfo>* pMergedInfo = NULL;
//  
//      LOG_FATAL_IF(mAssetPaths.size() == 0, "No assets added to AssetManager");
//      assert(dirName != NULL);
//  
//      //printf("+++ openDir(%s) in '%s'\n", dirName, (final char*) mAssetBase);
//  
//      pDir = new AssetDir;
//  
//      /*
//       * Scan the various directories, merging what we find into a single
//       * vector.  We want to scan them in reverse priority order so that
//       * the ".EXCLUDE" processing works correctly.  Also, if we decide we
//       * want to remember where the file is coming from, we'll get the right
//       * version.
//       *
//       * We start with Zip archives, then do loose files.
//       */
//      pMergedInfo = new SortedVector<AssetDir.FileInfo>;
//  
//      int i = mAssetPaths.size();
//      while (i > 0) {
//          i--;
//          final asset_path& ap = mAssetPaths.itemAt(i);
//          if (ap.type == kFileTypeRegular) {
//              ALOGV("Adding directory %s from zip %s", dirName, ap.path.string());
//              scanAndMergeZipLocked(pMergedInfo, ap, kAssetsRoot, dirName);
//          } else {
//              ALOGV("Adding directory %s from dir %s", dirName, ap.path.string());
//              scanAndMergeDirLocked(pMergedInfo, ap, kAssetsRoot, dirName);
//          }
//      }
//  
//  #if 0
//      printf("FILE LIST:\n");
//      for (i = 0; i < (int) pMergedInfo.size(); i++) {
//          printf(" %d: (%d) '%s'\n", i,
//              pMergedInfo.itemAt(i).getFileType(),
//              (final char*) pMergedInfo.itemAt(i).getFileName());
//      }
//  #endif
//  
//      pDir.setFileList(pMergedInfo);
//      return pDir;
//  }
//  
//  /*
//   * Open a directory in the non-asset namespace.
//   *
//   * An "asset directory" is simply the combination of all asset paths' "assets/" directories.
//   *
//   * Pass in "" for the root dir.
//   */
//  AssetDir* openNonAssetDir(final int cookie, final char* dirName)
//  {
//      AutoMutex _l(mLock);
//  
//      AssetDir* pDir = NULL;
//      SortedVector<AssetDir.FileInfo>* pMergedInfo = NULL;
//  
//      LOG_FATAL_IF(mAssetPaths.size() == 0, "No assets added to AssetManager");
//      assert(dirName != NULL);
//  
//      //printf("+++ openDir(%s) in '%s'\n", dirName, (final char*) mAssetBase);
//  
//      pDir = new AssetDir;
//  
//      pMergedInfo = new SortedVector<AssetDir.FileInfo>;
//  
//      final int which = static_cast<int>(cookie) - 1;
//  
//      if (which < mAssetPaths.size()) {
//          final asset_path& ap = mAssetPaths.itemAt(which);
//          if (ap.type == kFileTypeRegular) {
//              ALOGV("Adding directory %s from zip %s", dirName, ap.path.string());
//              scanAndMergeZipLocked(pMergedInfo, ap, NULL, dirName);
//          } else {
//              ALOGV("Adding directory %s from dir %s", dirName, ap.path.string());
//              scanAndMergeDirLocked(pMergedInfo, ap, NULL, dirName);
//          }
//      }
//  
//  #if 0
//      printf("FILE LIST:\n");
//      for (i = 0; i < (int) pMergedInfo.size(); i++) {
//          printf(" %d: (%d) '%s'\n", i,
//              pMergedInfo.itemAt(i).getFileType(),
//              (final char*) pMergedInfo.itemAt(i).getFileName());
//      }
//  #endif
//  
//      pDir.setFileList(pMergedInfo);
//      return pDir;
//  }
//  
//  /*
//   * Scan the contents of the specified directory and merge them into the
//   * "pMergedInfo" vector, removing previous entries if we find "exclude"
//   * directives.
//   *
//   * Returns "false" if we found nothing to contribute.
//   */
//  boolean scanAndMergeDirLocked(SortedVector<AssetDir.FileInfo>* pMergedInfo,
//      final asset_path& ap, final char* rootDir, final char* dirName)
//  {
//      assert(pMergedInfo != NULL);
//  
//      //printf("scanAndMergeDir: %s %s %s\n", ap.path.string(), rootDir, dirName);
//  
//      String8 path = createPathNameLocked(ap, rootDir);
//      if (dirName[0] != '\0')
//          path.appendPath(dirName);
//  
//      SortedVector<AssetDir.FileInfo>* pContents = scanDirLocked(path);
//      if (pContents == NULL)
//          return false;
//  
//      // if we wanted to do an incremental cache fill, we would do it here
//  
//      /*
//       * Process "exclude" directives.  If we find a filename that ends with
//       * ".EXCLUDE", we look for a matching entry in the "merged" set, and
//       * remove it if we find it.  We also delete the "exclude" entry.
//       */
//      int i, count, exclExtLen;
//  
//      count = pContents.size();
//      exclExtLen = strlen(kExcludeExtension);
//      for (i = 0; i < count; i++) {
//          final char* name;
//          int nameLen;
//  
//          name = pContents.itemAt(i).getFileName().string();
//          nameLen = strlen(name);
//          if (nameLen > exclExtLen &&
//              strcmp(name + (nameLen - exclExtLen), kExcludeExtension) == 0)
//          {
//              String8 match(name, nameLen - exclExtLen);
//              int matchIdx;
//  
//              matchIdx = AssetDir.FileInfo.findEntry(pMergedInfo, match);
//              if (matchIdx > 0) {
//                  ALOGV("Excluding '%s' [%s]\n",
//                      pMergedInfo.itemAt(matchIdx).getFileName().string(),
//                      pMergedInfo.itemAt(matchIdx).getSourceName().string());
//                  pMergedInfo.removeAt(matchIdx);
//              } else {
//                  //printf("+++ no match on '%s'\n", (final char*) match);
//              }
//  
//              ALOGD("HEY: size=%d removing %d\n", (int)pContents.size(), i);
//              pContents.removeAt(i);
//              i--;        // adjust "for" loop
//              count--;    //  and loop limit
//          }
//      }
//  
//      mergeInfoLocked(pMergedInfo, pContents);
//  
//      delete pContents;
//  
//      return true;
//  }
//  
//  /*
//   * Scan the contents of the specified directory, and stuff what we find
//   * into a newly-allocated vector.
//   *
//   * Files ending in ".gz" will have their extensions removed.
//   *
//   * We should probably think about skipping files with "illegal" names,
//   * e.g. illegal characters (/\:) or excessive length.
//   *
//   * Returns NULL if the specified directory doesn't exist.
//   */
//  SortedVector<AssetDir.FileInfo>* scanDirLocked(final String8& path)
//  {
//      SortedVector<AssetDir.FileInfo>* pContents = NULL;
//      DIR* dir;
//      struct dirent* entry;
//      FileType fileType;
//  
//      ALOGV("Scanning dir '%s'\n", path.string());
//  
//      dir = opendir(path.string());
//      if (dir == NULL)
//          return NULL;
//  
//      pContents = new SortedVector<AssetDir.FileInfo>;
//  
//      while (1) {
//          entry = readdir(dir);
//          if (entry == NULL)
//              break;
//  
//          if (strcmp(entry.d_name, ".") == 0 ||
//              strcmp(entry.d_name, "..") == 0)
//              continue;
//  
//  #ifdef _DIRENT_HAVE_D_TYPE
//          if (entry.d_type == DT_REG)
//              fileType = kFileTypeRegular;
//          else if (entry.d_type == DT_DIR)
//              fileType = kFileTypeDirectory;
//          else
//              fileType = kFileTypeUnknown;
//  #else
//          // stat the file
//          fileType = .getFileType(path.appendPathCopy(entry.d_name).string());
//  #endif
//  
//          if (fileType != kFileTypeRegular && fileType != kFileTypeDirectory)
//              continue;
//  
//          AssetDir.FileInfo info;
//          info.set(String8(entry.d_name), fileType);
//          if (strcasecmp(info.getFileName().getPathExtension().string(), ".gz") == 0)
//              info.setFileName(info.getFileName().getBasePath());
//          info.setSourceName(path.appendPathCopy(info.getFileName()));
//          pContents.add(info);
//      }
//  
//      closedir(dir);
//      return pContents;
//  }
//  
//  /*
//   * Scan the contents out of the specified Zip archive, and merge what we
//   * find into "pMergedInfo".  If the Zip archive in question doesn't exist,
//   * we return immediately.
//   *
//   * Returns "false" if we found nothing to contribute.
//   */
//  boolean scanAndMergeZipLocked(SortedVector<AssetDir.FileInfo>* pMergedInfo,
//      final asset_path& ap, final char* rootDir, final char* baseDirName)
//  {
//      ZipFileRO* pZip;
//      Vector<String8> dirs;
//      AssetDir.FileInfo info;
//      SortedVector<AssetDir.FileInfo> contents;
//      String8 sourceName, zipName, dirName;
//  
//      pZip = mZipSet.getZip(ap.path);
//      if (pZip == NULL) {
//          ALOGW("Failure opening zip %s\n", ap.path.string());
//          return false;
//      }
//  
//      zipName = ZipSet.getPathName(ap.path.string());
//  
//      /* convert "sounds" to "rootDir/sounds" */
//      if (rootDir != NULL) dirName = rootDir;
//      dirName.appendPath(baseDirName);
//  
//      /*
//       * Scan through the list of files, looking for a match.  The files in
//       * the Zip table of contents are not in sorted order, so we have to
//       * process the entire list.  We're looking for a string that begins
//       * with the characters in "dirName", is followed by a '/', and has no
//       * subsequent '/' in the stuff that follows.
//       *
//       * What makes this especially fun is that directories are not stored
//       * explicitly in Zip archives, so we have to infer them from context.
//       * When we see "sounds/foo.wav" we have to leave a note to ourselves
//       * to insert a directory called "sounds" into the list.  We store
//       * these in temporary vector so that we only return each one once.
//       *
//       * Name comparisons are case-sensitive to match UNIX filesystem
//       * semantics.
//       */
//      int dirNameLen = dirName.length();
//      void *iterationCookie;
//      if (!pZip.startIteration(&iterationCookie, dirName.string(), NULL)) {
//          ALOGW("ZipFileRO.startIteration returned false");
//          return false;
//      }
//  
//      ZipEntryRO entry;
//      while ((entry = pZip.nextEntry(iterationCookie)) != NULL) {
//          char nameBuf[256];
//  
//          if (pZip.getEntryFileName(entry, nameBuf, sizeof(nameBuf)) != 0) {
//              // TODO: fix this if we expect to have long names
//              ALOGE("ARGH: name too long?\n");
//              continue;
//          }
//          //printf("Comparing %s in %s?\n", nameBuf, dirName.string());
//          if (dirNameLen == 0 || nameBuf[dirNameLen] == '/')
//          {
//              final char* cp;
//              final char* nextSlash;
//  
//              cp = nameBuf + dirNameLen;
//              if (dirNameLen != 0)
//                  cp++;       // advance past the '/'
//  
//              nextSlash = strchr(cp, '/');
//  //xxx this may break if there are bare directory entries
//              if (nextSlash == NULL) {
//                  /* this is a file in the requested directory */
//  
//                  info.set(String8(nameBuf).getPathLeaf(), kFileTypeRegular);
//  
//                  info.setSourceName(
//                      createZipSourceNameLocked(zipName, dirName, info.getFileName()));
//  
//                  contents.add(info);
//                  //printf("FOUND: file '%s'\n", info.getFileName().string());
//              } else {
//                  /* this is a subdir; add it if we don't already have it*/
//                  String8 subdirName(cp, nextSlash - cp);
//                  int j;
//                  int N = dirs.size();
//  
//                  for (j = 0; j < N; j++) {
//                      if (subdirName == dirs[j]) {
//                          break;
//                      }
//                  }
//                  if (j == N) {
//                      dirs.add(subdirName);
//                  }
//  
//                  //printf("FOUND: dir '%s'\n", subdirName.string());
//              }
//          }
//      }
//  
//      pZip.endIteration(iterationCookie);
//  
//      /*
//       * Add the set of unique directories.
//       */
//      for (int i = 0; i < (int) dirs.size(); i++) {
//          info.set(dirs[i], kFileTypeDirectory);
//          info.setSourceName(
//              createZipSourceNameLocked(zipName, dirName, info.getFileName()));
//          contents.add(info);
//      }
//  
//      mergeInfoLocked(pMergedInfo, &contents);
//  
//      return true;
//  }
//  
//  
//  /*
//   * Merge two vectors of FileInfo.
//   *
//   * The merged contents will be stuffed into *pMergedInfo.
//   *
//   * If an entry for a file exists in both "pMergedInfo" and "pContents",
//   * we use the newer "pContents" entry.
//   */
//  void mergeInfoLocked(SortedVector<AssetDir.FileInfo>* pMergedInfo,
//      final SortedVector<AssetDir.FileInfo>* pContents)
//  {
//      /*
//       * Merge what we found in this directory with what we found in
//       * other places.
//       *
//       * Two basic approaches:
//       * (1) Create a new array that holds the unique values of the two
//       *     arrays.
//       * (2) Take the elements from pContents and shove them into pMergedInfo.
//       *
//       * Because these are vectors of complex objects, moving elements around
//       * inside the vector requires finalructing new objects and allocating
//       * storage for members.  With approach #1, we're always adding to the
//       * end, whereas with #2 we could be inserting multiple elements at the
//       * front of the vector.  Approach #1 requires a full copy of the
//       * contents of pMergedInfo, but approach #2 requires the same copy for
//       * every insertion at the front of pMergedInfo.
//       *
//       * (We should probably use a SortedVector interface that allows us to
//       * just stuff items in, trusting us to maintain the sort order.)
//       */
//      SortedVector<AssetDir.FileInfo>* pNewSorted;
//      int mergeMax, contMax;
//      int mergeIdx, contIdx;
//  
//      pNewSorted = new SortedVector<AssetDir.FileInfo>;
//      mergeMax = pMergedInfo.size();
//      contMax = pContents.size();
//      mergeIdx = contIdx = 0;
//  
//      while (mergeIdx < mergeMax || contIdx < contMax) {
//          if (mergeIdx == mergeMax) {
//              /* hit end of "merge" list, copy rest of "contents" */
//              pNewSorted.add(pContents.itemAt(contIdx));
//              contIdx++;
//          } else if (contIdx == contMax) {
//              /* hit end of "cont" list, copy rest of "merge" */
//              pNewSorted.add(pMergedInfo.itemAt(mergeIdx));
//              mergeIdx++;
//          } else if (pMergedInfo.itemAt(mergeIdx) == pContents.itemAt(contIdx))
//          {
//              /* items are identical, add newer and advance both indices */
//              pNewSorted.add(pContents.itemAt(contIdx));
//              mergeIdx++;
//              contIdx++;
//          } else if (pMergedInfo.itemAt(mergeIdx) < pContents.itemAt(contIdx))
//          {
//              /* "merge" is lower, add that one */
//              pNewSorted.add(pMergedInfo.itemAt(mergeIdx));
//              mergeIdx++;
//          } else {
//              /* "cont" is lower, add that one */
//              assert(pContents.itemAt(contIdx) < pMergedInfo.itemAt(mergeIdx));
//              pNewSorted.add(pContents.itemAt(contIdx));
//              contIdx++;
//          }
//      }
//  
//      /*
//       * Overwrite the "merged" list with the new stuff.
//       */
//      *pMergedInfo = *pNewSorted;
//      delete pNewSorted;
//  
//  #if 0       // for Vector, rather than SortedVector
//      int i, j;
//      for (i = pContents.size() -1; i >= 0; i--) {
//          boolean add = true;
//  
//          for (j = pMergedInfo.size() -1; j >= 0; j--) {
//              /* case-sensitive comparisons, to behave like UNIX fs */
//              if (strcmp(pContents.itemAt(i).mFileName,
//                         pMergedInfo.itemAt(j).mFileName) == 0)
//              {
//                  /* match, don't add this entry */
//                  add = false;
//                  break;
//              }
//          }
//  
//          if (add)
//              pMergedInfo.add(pContents.itemAt(i));
//      }
//  #endif
//  }
//  
//  /*
//   * ===========================================================================
//   *      SharedZip
//   * ===========================================================================
//   */
//  
//  
//  Mutex SharedZip.gLock;
//  DefaultKeyedVector<String8, wp<SharedZip> > SharedZip.gOpen;
//  
//  SharedZip.SharedZip(final String8& path, time_t modWhen)
//      : mPath(path), mZipFile(NULL), mModWhen(modWhen),
//        mResourceTableAsset(NULL), mResourceTable(NULL)
//  {
//      if (kIsDebug) {
//          ALOGI("Creating SharedZip %p %s\n", this, (final char*)mPath);
//      }
//      ALOGV("+++ opening zip '%s'\n", mPath.string());
//      mZipFile = ZipFileRO.open(mPath.string());
//      if (mZipFile == NULL) {
//          ALOGD("failed to open Zip archive '%s'\n", mPath.string());
//      }
//  }
//  
//  sp<SharedZip> SharedZip.get(final String8& path,
//          boolean createIfNotPresent)
//  {
//      AutoMutex _l(gLock);
//      time_t modWhen = getFileModDate(path);
//      sp<SharedZip> zip = gOpen.valueFor(path).promote();
//      if (zip != NULL && zip.mModWhen == modWhen) {
//          return zip;
//      }
//      if (zip == NULL && !createIfNotPresent) {
//          return NULL;
//      }
//      zip = new SharedZip(path, modWhen);
//      gOpen.add(path, zip);
//      return zip;
//  
//  }
//  
//  ZipFileRO* SharedZip.getZip()
//  {
//      return mZipFile;
//  }
//  
//  Asset* SharedZip.getResourceTableAsset()
//  {
//      AutoMutex _l(gLock);
//      ALOGV("Getting from SharedZip %p resource asset %p\n", this, mResourceTableAsset);
//      return mResourceTableAsset;
//  }
//  
//  Asset* SharedZip.setResourceTableAsset(Asset* asset)
//  {
//      {
//          AutoMutex _l(gLock);
//          if (mResourceTableAsset == NULL) {
//              // This is not thread safe the first time it is called, so
//              // do it here with the global lock held.
//              asset.getBuffer(true);
//              mResourceTableAsset = asset;
//              return asset;
//          }
//      }
//      delete asset;
//      return mResourceTableAsset;
//  }
//  
//  ResTable* SharedZip.getResourceTable()
//  {
//      ALOGV("Getting from SharedZip %p resource table %p\n", this, mResourceTable);
//      return mResourceTable;
//  }
//  
//  ResTable* SharedZip.setResourceTable(ResTable* res)
//  {
//      {
//          AutoMutex _l(gLock);
//          if (mResourceTable == NULL) {
//              mResourceTable = res;
//              return res;
//          }
//      }
//      delete res;
//      return mResourceTable;
//  }
//  
//  boolean SharedZip.isUpToDate()
//  {
//      time_t modWhen = getFileModDate(mPath.string());
//      return mModWhen == modWhen;
//  }
//  
//  void SharedZip.addOverlay(final asset_path& ap)
//  {
//      mOverlays.add(ap);
//  }
//  
//  boolean SharedZip.getOverlay(int idx, asset_path* out) final
//  {
//      if (idx >= mOverlays.size()) {
//          return false;
//      }
//      *out = mOverlays[idx];
//      return true;
//  }
//  
//  SharedZip.~SharedZip()
//  {
//      if (kIsDebug) {
//          ALOGI("Destroying SharedZip %p %s\n", this, (final char*)mPath);
//      }
//      if (mResourceTable != NULL) {
//          delete mResourceTable;
//      }
//      if (mResourceTableAsset != NULL) {
//          delete mResourceTableAsset;
//      }
//      if (mZipFile != NULL) {
//          delete mZipFile;
//          ALOGV("Closed '%s'\n", mPath.string());
//      }
//  }
//  
//  /*
//   * ===========================================================================
//   *      ZipSet
//   * ===========================================================================
//   */
//  
//  /*
//   * Destructor.  Close any open archives.
//   */
//  ZipSet.~ZipSet(void)
//  {
//      int N = mZipFile.size();
//      for (int i = 0; i < N; i++)
//          closeZip(i);
//  }
//  
//  /*
//   * Close a Zip file and reset the entry.
//   */
//  void ZipSet.closeZip(int idx)
//  {
//      mZipFile.editItemAt(idx) = NULL;
//  }
//  
//  
//  /*
//   * Retrieve the appropriate Zip file from the set.
//   */
//  ZipFileRO* ZipSet.getZip(final String8& path)
//  {
//      int idx = getIndex(path);
//      sp<SharedZip> zip = mZipFile[idx];
//      if (zip == NULL) {
//          zip = SharedZip.get(path);
//          mZipFile.editItemAt(idx) = zip;
//      }
//      return zip.getZip();
//  }
//  
//  Asset* ZipSet.getZipResourceTableAsset(final String8& path)
//  {
//      int idx = getIndex(path);
//      sp<SharedZip> zip = mZipFile[idx];
//      if (zip == NULL) {
//          zip = SharedZip.get(path);
//          mZipFile.editItemAt(idx) = zip;
//      }
//      return zip.getResourceTableAsset();
//  }
//  
//  Asset* ZipSet.setZipResourceTableAsset(final String8& path,
//                                                   Asset* asset)
//  {
//      int idx = getIndex(path);
//      sp<SharedZip> zip = mZipFile[idx];
//      // doesn't make sense to call before previously accessing.
//      return zip.setResourceTableAsset(asset);
//  }
//  
//  ResTable* ZipSet.getZipResourceTable(final String8& path)
//  {
//      int idx = getIndex(path);
//      sp<SharedZip> zip = mZipFile[idx];
//      if (zip == NULL) {
//          zip = SharedZip.get(path);
//          mZipFile.editItemAt(idx) = zip;
//      }
//      return zip.getResourceTable();
//  }
//  
//  ResTable* ZipSet.setZipResourceTable(final String8& path,
//                                                      ResTable* res)
//  {
//      int idx = getIndex(path);
//      sp<SharedZip> zip = mZipFile[idx];
//      // doesn't make sense to call before previously accessing.
//      return zip.setResourceTable(res);
//  }
//  
//  /*
//   * Generate the partial pathname for the specified archive.  The caller
//   * gets to prepend the asset root directory.
//   *
//   * Returns something like "common/en-US-noogle.jar".
//   */
//  /*static*/ String8 ZipSet.getPathName(final char* zipPath)
//  {
//      return String8(zipPath);
//  }
//  
//  boolean ZipSet.isUpToDate()
//  {
//      final int N = mZipFile.size();
//      for (int i=0; i<N; i++) {
//          if (mZipFile[i] != NULL && !mZipFile[i].isUpToDate()) {
//              return false;
//          }
//      }
//      return true;
//  }
//  
//  void ZipSet.addOverlay(final String8& path, final asset_path& overlay)
//  {
//      int idx = getIndex(path);
//      sp<SharedZip> zip = mZipFile[idx];
//      zip.addOverlay(overlay);
//  }
//  
//  boolean ZipSet.getOverlay(final String8& path, int idx, asset_path* out) final
//  {
//      sp<SharedZip> zip = SharedZip.get(path, false);
//      if (zip == NULL) {
//          return false;
//      }
//      return zip.getOverlay(idx, out);
//  }
//  
//  /*
//   * Compute the zip file's index.
//   *
//   * "appName", "locale", and "vendor" should be set to NULL to indicate the
//   * default directory.
//   */
//  int ZipSet.getIndex(final String8& zip) final
//  {
//      final int N = mZipPath.size();
//      for (int i=0; i<N; i++) {
//          if (mZipPath[i] == zip) {
//              return i;
//          }
//      }
//  
//      mZipPath.add(zip);
//      mZipFile.add(NULL);
//  
//      return mZipPath.size()-1;
//  }
                                                                                  
}