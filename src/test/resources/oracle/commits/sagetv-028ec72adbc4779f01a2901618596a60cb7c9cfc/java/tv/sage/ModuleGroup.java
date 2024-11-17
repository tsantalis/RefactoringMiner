/*
 * Copyright 2015 The SageTV Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tv.sage;

import sage.Widget;
/**
 * @author 601
 */
public class ModuleGroup
{
  protected static final String DEFAULT_STV_FILENAME_KEY = "wizard/widget_db_file";


  // instance

  // (moduleName-String, Module)
  public final java.util.Map moduleMap = new java.util.TreeMap(new java.util.Comparator()
  {
    // by name only
    public int compare(Object o1, Object o2)
    {
      int diff = ((String)o1).compareTo((String)o2);   //ZQ. JDK 1.5 comaptiable

      //System.out.println("compare o1=" + o1 + " o2=" + o2 + " diff=" + diff);

      return (diff);
    }
  });

  // (module:symbol-String, Widget)
  public java.util.Map<String, Widget> symbolMap = new java.util.HashMap<String, Widget>();

  // 601 Is this needed?
  public tv.sage.mod.Module defaultModule = null;

  private java.util.Map breakIdMap = null;

  public ModuleGroup()
  {
  }

  public void dispose()
  {
    // 601 cleanup!
  }

  public Modular[] getModules()
  {
    return ((Modular[])moduleMap.values().toArray(new Modular[moduleMap.size()]));
  }

  public sage.Widget[] getWidgets()
  {
    // 601 do all modules?
    return (defaultModule.getWidgetz());
  }

  public sage.Widget[] getWidgets(byte type)
  {
    // 601 do all modules?
    return (defaultModule.findWidgetz(type));
  }

  public sage.Widget getWidgetForID(int id)
  {
    return defaultModule.getWidgetForId(id);
  }

  public long lastModified()
  {
    return defaultModule.lastModified();
  }

  public sage.Widget addWidget(byte type)
  {
    sage.Widget rv = (defaultModule.addWidget(type, null));
    if (rv.symbol() != null)
      symbolMap.put(rv.symbol(), rv);
    return rv;
  }

  public sage.Widget addWidget(byte type, String symbol)
  {
    sage.Widget rv = (defaultModule.addWidget(type, symbol));
    if (rv.symbol() != null)
      symbolMap.put(rv.symbol(), rv);
    return rv;
  }

  public sage.Widget klone(sage.Widget w)
  {
    sage.Widget rv = defaultModule.kloneWidget(w);
    if (rv.symbol() != null)
      symbolMap.put(rv.symbol(), rv);
    return rv;
  }

  public void removeWidget(sage.Widget w)
  {
    symbolMap.remove(w.symbol());
    defaultModule.removeWidget(w);
  }

  public void resurrectWidget(sage.Widget w)
  {
    defaultModule.resurrectWidget(w);
    if (w.symbol() != null)
      symbolMap.put(w.symbol(), w);
  }

  public void retranslate()
  {
    defaultModule.retranslate();
  }

  // Cache the last one loaded in case we reload it again so it'll be quick!
  private static String cachedModuleName;
  private static tv.sage.mod.Module cachedModule;
  private static java.util.Map<String, Widget> cachedSymbolMap = new java.util.HashMap<String, Widget>();
  private static long timestampCachedModule;
  private static long timestampCachedModuleFile;

  public void load(java.util.Properties moduleProperties) throws tv.sage.SageException
  {
    if (moduleProperties == null)
    {
      tv.sage.mod.Module mod = new tv.sage.mod.Module("default");
      moduleMap.put(mod.name(), mod);
      defaultModule = mod;
      mod.setChanged();
      mod.setModuleGroup(this);
    }
    else if (moduleProperties.size() == 0)
    {
      String stvFilename = sage.Sage.get(DEFAULT_STV_FILENAME_KEY, null);

      tv.sage.mod.Module mod = tv.sage.mod.Module.loadSTV(new java.io.File(stvFilename));

      moduleMap.put(mod.name(), mod);

      defaultModule = mod;
      mod.setModuleGroup(this);
    }
    else if (moduleProperties.getProperty("STV") != null)
    {
      String stvFilename = sage.Sage.get(DEFAULT_STV_FILENAME_KEY, null);

      if (!"true".equalsIgnoreCase(moduleProperties.getProperty("STV")))
      {
        stvFilename = moduleProperties.getProperty("STV");
      }

      java.io.File file = new java.io.File(stvFilename);
      if (sage.Sage.EMBEDDED && stvFilename.equals(cachedModuleName) && cachedModule != null &&
          cachedModule.lastModified() < timestampCachedModule &&
          timestampCachedModuleFile >= file.lastModified())
      {
        if (sage.Sage.DBG) System.out.println("Using cached STV module to speed loading!");
        moduleMap.put(cachedModule.name(), cachedModule);

        defaultModule = cachedModule;
        cachedModule.setModuleGroup(this);
        return;
      }

      // Peek at the first 3 bytes of the file to see if it's an STV file
      boolean isSTVFile = isWIZFile(file);
      //			java.io.FileInputStream fis = null;
      //			try
      //			{
      //				fis = new java.io.FileInputStream(stvFilename);
      //				int b1 = fis.read();
      //				int b2 = fis.read();
      //				int b3 = fis.read();
      //				if (b1 == 'W' && b2 == 'I' && b3 == 'Z')
      //					isSTVFile = true;
      //			}
      //			catch (Exception e)
      //			{
      //				System.out.println("Error reading STV file of:" + e);
      //			}
      //			finally
      //			{
      //				if (fis != null)
      //				{
      //					try{fis.close();}catch(Exception e){}
      //					fis = null;
      //				}
      //			}
      tv.sage.mod.Module mod;

      if (isSTVFile)
      {
        mod = tv.sage.mod.Module.loadSTV(file);
      }
      else if (file.getName().equalsIgnoreCase("skin.xml"))
			{
				mod = tv.sage.mod.Module.loadXBMC(file);
			}
      else
      {
        mod = tv.sage.mod.Module.loadXML(this, symbolMap, file);
      }

      moduleMap.put(mod.name(), mod);

      defaultModule = mod;
      if (sage.Sage.EMBEDDED)
      {
        mod.setModuleGroup(this);

        cachedModuleName = stvFilename;
        timestampCachedModule = sage.Sage.time();
        cachedModule = mod;
        timestampCachedModuleFile = file.lastModified() + 10000; //offset it to account for filesystem differences
      }
    }
    else // XML
    {
      String modules = moduleProperties.getProperty("modules");

      if (modules == null) modules = "default";

      java.util.StringTokenizer st = new java.util.StringTokenizer(modules, ",");
      while (st.hasMoreTokens())
      {
        String moduleName = st.nextToken();

        tv.sage.mod.Module mod = tv.sage.mod.Module.loadXML(this, symbolMap, new java.io.File("xml/" + moduleName + ".xml"));

        moduleMap.put(mod.name(), mod);

        if (defaultModule == null)
        {
          defaultModule = mod;
        }
      }
    }
  }

  public void importXML(java.io.File file, sage.UIManager uiMan) throws tv.sage.SageException
  {
    if (defaultModule != null)
    {
      defaultModule.importXML(symbolMap, file, uiMan);
    }
  }

  public void exportXML(java.util.Collection widgets, java.io.File file) throws tv.sage.SageException
  {
    if (defaultModule != null)
    {
      defaultModule.exportXML(widgets, file);
    }
  }

  public void exportSTV(java.io.File file) throws tv.sage.SageException
  {
    exportSTV(file, null);
  }
  public void exportSTV(java.io.File file, Object cryptoKeyObj) throws tv.sage.SageException
  {
    sage.Widget[] allWidgs = getWidgets();
    if (sage.Sage.DBG) System.out.println("Compressing widgets to STV file: " + file);
    long fp;
    boolean optimize = file.toString().endsWith(".opt.stv");
    try
    {
      byte[] cryptoKey = null;
      if (cryptoKeyObj != null)
      {
        if (cryptoKeyObj instanceof byte[])
          cryptoKey = (byte[]) cryptoKeyObj;
        else if (cryptoKeyObj instanceof Object[])
        {
          Object[] objArr = (Object[]) cryptoKeyObj;
          cryptoKey = new byte[objArr.length];
          for (int i = 0; i < objArr.length; i++)
            cryptoKey[i] = Byte.parseByte(objArr[i].toString());
        }
      }
      file.createNewFile();
      sage.io.SageDataFile widgetDBout;

      if (cryptoKey == null)
        widgetDBout = new sage.io.SageDataFile(new sage.io.BufferedSageFile(new sage.io.LocalSageFile(file, "rwd")), sage.Sage.I18N_CHARSET);
      else
        widgetDBout = new sage.io.SageDataFile(new sage.io.EncryptedSageFile(new sage.io.BufferedSageFile(new sage.io.LocalSageFile(file, "rwd")), cryptoKey), sage.Sage.I18N_CHARSET);

      widgetDBout.writeUnencryptedByte((byte) 'W');
      widgetDBout.writeUnencryptedByte((byte) 'I');
      widgetDBout.writeUnencryptedByte((byte) (optimize ? 'X' : 'Z'));
      widgetDBout.flush();

      // The BAD_VERSION marker is to signify incompletely saved DB files.
      long verPos = widgetDBout.position();
      widgetDBout.writeUnencryptedByte(sage.Wizard.BAD_VERSION);
      widgetDBout.flush();

      fp = widgetDBout.position();
      widgetDBout.writeInt(Integer.MAX_VALUE);
      widgetDBout.writeByte(sage.Wizard.SIZE);
      widgetDBout.writeByte(sage.Wizard.WIDGET_CODE);
      widgetDBout.writeInt(allWidgs.length);
      sage.Wizard.logCmdLength(widgetDBout, fp);
      if (allWidgs.length > 0)
      {
        fp = widgetDBout.position();
        widgetDBout.writeInt(Integer.MAX_VALUE);
        widgetDBout.writeByte(sage.Wizard.FULL_DATA);
        widgetDBout.writeByte(sage.Wizard.WIDGET_CODE);
        boolean writeSyms = optimize && sage.Sage.getBoolean("studio/write_stv_opt_widget_symbols", false);
        for (int j = 0; j < allWidgs.length; j++)
        {
          widgetDBout.writeInt(allWidgs[j].id());
          if (optimize)
            widgetDBout.writeByte(allWidgs[j].type());
          else
            widgetDBout.writeUTF(sage.Widget.TYPES[allWidgs[j].type()]);
          int numProps = writeSyms ? 2 : 1;
          for (byte i = 0; i <= sage.Widget.MAX_PROP_NUM; i++)
            if (allWidgs[j].hasProperty(i))
              numProps++;
          widgetDBout.writeInt(numProps);
          if (!optimize)
            widgetDBout.writeUTF("Name");
          widgetDBout.writeUTF(allWidgs[j].getUntranslatedName());
          for (byte i = 0; i <= sage.Widget.MAX_PROP_NUM; i++)
            if (allWidgs[j].hasProperty(i))
            {
              if (optimize)
                widgetDBout.writeByte(i);
              else
                widgetDBout.writeUTF(sage.Widget.PROPS[i]);
              widgetDBout.writeUTF(allWidgs[j].getProperty(i));
            }
          if (writeSyms)
          {
            widgetDBout.writeByte(0xFF);
            widgetDBout.writeUTF(allWidgs[j].symbol());
          }
          sage.Widget[] kids = allWidgs[j].contents();
          widgetDBout.writeInt(kids.length);
          for (int i = 0; i < kids.length; i++)
            widgetDBout.writeInt(kids[i].id());
          sage.Widget[] parents = allWidgs[j].containers();
          widgetDBout.writeInt(parents.length);
          for (int i = 0; i < parents.length; i++)
            widgetDBout.writeInt(parents[i].id());
        }
        sage.Wizard.logCmdLength(widgetDBout, fp);
      }
      fp = widgetDBout.position();
      widgetDBout.setLength(fp);
      widgetDBout.seek(verPos);
      widgetDBout.writeUnencryptedByte(cryptoKey != null ? (byte) 0x36 : (byte)0x20); // < 0x2F to signify unencrypted
      widgetDBout.flush();
      widgetDBout.close();
    }
    catch (java.io.IOException ioe)
    {
      throw new tv.sage.SageException(ioe, 0);
    }
  }

  /** Peek at the first 3 bytes of the file to see if it's an STV file */
  public static boolean isWIZFile(java.io.File file)
  {
    try
    {
      java.io.FileInputStream fis = new java.io.FileInputStream(file);

      try
      {
        int b1 = fis.read();
        int b2 = fis.read();
        int b3 = fis.read();

        if (b1 == 'W' && b2 == 'I' && (b3 == 'Z' || b3 == 'X'))
        {
          return (true);
        }

        return (false);
      }
      finally
      {
        fis.close();
      }
    }
    catch (Exception x)
    {
      return (false);
    }
  }

  public void setBreakIdMap(java.util.Map breakIdMap)
  {
    this.breakIdMap = breakIdMap;
  }

  public java.util.Map getBreakIdMap()
  {
    return (breakIdMap);
  }
}
