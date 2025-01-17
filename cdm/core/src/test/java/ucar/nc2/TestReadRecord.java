/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.StructureDS;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class TestReadRecord {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  // Normal reading of Nc3 record variables
  public void testNC3ReadRecordVariables() throws IOException, InvalidRangeException {
    try (NetcdfFile nc = TestDir.openFileLocal("testWriteRecord.nc")) {

      /* Get the value of the global attribute named "title" */
      String title = nc.getRootGroup().findAttributeString("title", "N/A");

      /*
       * Read the latitudes into an array of double.
       * This works regardless of the external
       * type of the "lat" variable.
       */
      Variable lat = nc.findVariable("lat");
      assert lat != null;
      assert (lat.getRank() == 1); // make sure it's 1-dimensional
      int nlats = lat.getShape()[0]; // number of latitudes
      double[] lats = new double[nlats]; // where to put them

      Array values = lat.read(); // read all into memory
      Index ima = values.getIndex(); // index array to specify which value
      for (int ilat = 0; ilat < nlats; ilat++) {
        lats[ilat] = values.getDouble(ima.set0(ilat));
      }
      /* Read units attribute of lat variable */
      String latUnits = lat.findAttributeString("units", "N/A");
      assert (latUnits.equals("degrees_north"));

      /* Read the longitudes. */
      Variable lon = nc.findVariable("lon");
      assert lon != null;
      values = lon.read();
      assert (values instanceof ArrayFloat.D1);
      ArrayFloat.D1 fa = (ArrayFloat.D1) values;
      Assert2.assertNearlyEquals(fa.get(0), -109.0f);
      Assert2.assertNearlyEquals(fa.get(1), -107.0f);
      Assert2.assertNearlyEquals(fa.get(2), -105.0f);
      Assert2.assertNearlyEquals(fa.get(3), -103.0f);

      /*
       * Now we can just use the MultiArray to access values, or
       * we can copy the MultiArray elements to another array with
       * toArray(), or we can get access to the MultiArray storage
       * without copying. Each of these approaches to accessing
       * the data are illustrated below.
       */

      /* Read the times: unlimited dimension */
      Variable time = nc.findVariable("time");
      assert time != null;
      Array timeValues = time.read();
      assert (timeValues instanceof ArrayInt.D1);
      ArrayInt.D1 ta = (ArrayInt.D1) timeValues;
      assert (ta.get(0) == 6) : ta.get(0);
      assert (ta.get(1) == 18) : ta.get(1);

      /* Read the relative humidity data */
      Variable rh = nc.findVariable("rh");
      Array rhValues = rh.read();
      assert (rhValues instanceof ArrayInt.D3);
      ArrayInt.D3 rha = (ArrayInt.D3) rhValues;
      int[] shape = rha.getShape();
      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          for (int k = 0; k < shape[2]; k++) {
            int want = 20 * i + 4 * j + k + 1;
            int val = rha.get(i, j, k);
            assert (want == val) : val;
          }
        }
      }

      /* Read the temperature data */
      Variable t = nc.findVariable("T");
      Array tValues = t.read();
      assert (tValues instanceof ArrayDouble.D3);
      ArrayDouble.D3 Ta = (ArrayDouble.D3) tValues;
      Assert2.assertNearlyEquals(Ta.get(0, 0, 0), 1.0f);
      Assert2.assertNearlyEquals(Ta.get(1, 1, 1), 10.0f);

      /* Read subset of the temperature data */
      tValues = t.read(new int[3], new int[] {2, 2, 2});
      assert (tValues instanceof ArrayDouble.D3);
      Ta = (ArrayDouble.D3) tValues;
      Assert2.assertNearlyEquals(Ta.get(0, 0, 0), 1.0f);
      Assert2.assertNearlyEquals(Ta.get(1, 1, 1), 10.0f);
    }
  }

  @Test
  // Reading of Nc3 record variables, having been made into a structure
  public void testNC3ReadRecordsAsStructuture() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "testWriteRecord.nc", -1, null,
        NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      Variable record = ncfile.findVariable("record");
      assert record instanceof Structure;
      Structure rs = (Structure) record;
      assert rs.getRank() == 1;
      assert rs.getDimension(0).getLength() == 2;

      /* Read the records */
      Array rsValues = rs.read();
      assert (rsValues instanceof ArrayStructure);
      assert rsValues.getRank() == 1;
      assert rsValues.getShape()[0] == 2;

      /* Read the times: unlimited dimension */
      Variable time = rs.findVariable("time");
      Array timeValues = time.read();
      assert (timeValues instanceof ArrayInt.D0) : timeValues.getClass().getName();
      ArrayInt.D0 ta = (ArrayInt.D0) timeValues;
      assert (ta.get() == 6) : ta.get();

      /* Read the relative humidity data */
      Variable rh = rs.findVariable("rh");
      Array rhValues = rh.read();
      assert (rhValues instanceof ArrayInt.D2);
      ArrayInt.D2 rha = (ArrayInt.D2) rhValues;
      int[] shape = rha.getShape();
      // for (int i=0; i<shape[0]; i++) {
      for (int j = 0; j < shape[0]; j++) {
        for (int k = 0; k < shape[1]; k++) {
          int want = 4 * j + k + 1;
          int val = rha.get(j, k);
          assert (want == val) : val;
        }
      }
    }
  }


  @Test
  public void testNC3ReadRecordStridedOld() throws InvalidRangeException, IOException {
    // record variable
    try (NetcdfFile ncfile = NetcdfFile.open(TestDir.cdmLocalTestDataDir + "testWriteRecord.nc", -1, null,
        NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      Variable record = ncfile.findVariable("record");
      assert record instanceof Structure;
      Structure rs = (Structure) record;
      assert rs.getRank() == 1;
      assert rs.getDimension(0).getLength() == 2;

      /* Read a record */
      Array rsValues = rs.read("1:1:2");
      assert (rsValues instanceof ArrayStructure);
      assert rsValues.getRank() == 1;
      assert rsValues.getShape()[0] == 1;

      StructureData sdata = (StructureData) rsValues.getObject(rsValues.getIndex());
      Array gdata = sdata.getArray("time");
      assert gdata instanceof ArrayInt.D0 : gdata.getClass().getName();
      ArrayInt.D0 tdata = (ArrayInt.D0) gdata;
      int t = tdata.get();
      assert t == 18;

      int t2 = sdata.getScalarInt("time");
      assert t2 == 18;
    }
  }

  @Test
  public void testNC3ReadRecordStrided() throws InvalidRangeException, IOException {
    // record variable
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "testWriteRecord.nc", -1, null,
        NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      Variable record = ncfile.findVariable("record");
      assert record instanceof Structure;
      Structure rs = (Structure) record;
      assert rs.getRank() == 1;
      assert rs.getDimension(0).getLength() == 2;

      /* Read a record */
      Array rsValues = rs.read("1:1:2");
      assert (rsValues instanceof ArrayStructure);
      assert rsValues.getRank() == 1;
      assert rsValues.getShape()[0] == 1;

      StructureData sdata = (StructureData) rsValues.getObject(rsValues.getIndex());
      Array gdata = sdata.getArray("time");
      assert gdata instanceof ArrayInt.D0 : gdata.getClass().getName();
      ArrayInt.D0 tdata = (ArrayInt.D0) gdata;
      int t = tdata.get();
      assert t == 18;

      int t2 = sdata.getScalarInt("time");
      assert t2 == 18;
    }
  }

  @Test
  public void testDatasetAddRecord() throws InvalidRangeException, IOException {
    String location = TestDir.cdmLocalTestDataDir + "testWriteRecord.nc";
    DatasetUrl durl = DatasetUrl.create(null, location);
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(durl, NetcdfDataset.getDefaultEnhanceMode(), -1, null,
        NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {

      // record variable
      Variable record = ncd.findVariable("record");
      assert record instanceof StructureDS;
      StructureDS rs = (StructureDS) record;
      assert rs.getRank() == 1;
      assert rs.getDimension(0).getLength() == 2;

      /* Read a record */
      Array rsValues = rs.read("1:1:2");
      assert (rsValues instanceof ArrayStructure);
      assert rsValues.getRank() == 1;
      assert rsValues.getShape()[0] == 1;

      StructureData sdata = (StructureData) rsValues.getObject(rsValues.getIndex());
      Array gdata = sdata.getArray("time");
      assert gdata instanceof ArrayInt.D0 : gdata.getClass().getName();
      ArrayInt.D0 tdata = (ArrayInt.D0) gdata;
      int t = tdata.get();
      assert t == 18;

      int t2 = sdata.getScalarInt("time");
      assert t2 == 18;

      /* Read the times: unlimited dimension */
      Variable time = rs.findVariable("time");
      assert time != null;
      Array timeValues = time.read();
      assert (timeValues instanceof ArrayInt.D0);
      ArrayInt.D0 ta = (ArrayInt.D0) timeValues;
      assert (ta.get() == 6) : ta.get();
    }
  }

  // This only works on old iosp
  @Test
  public void testDatasetAddRecordAfter() throws InvalidRangeException, IOException {
    try (NetcdfDataset ncd = NetcdfDataset.openDataset(TestDir.cdmLocalTestDataDir + "testWriteRecord.nc")) {
      assert (Boolean) ncd.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

      // record variable
      Variable record = ncd.findVariable("record");
      assert record instanceof StructureDS;
      StructureDS rs = (StructureDS) record;
      assert rs.getRank() == 1;
      assert rs.getDimension(0).getLength() == 2;

      /* Read a record */
      Array rsValues = rs.read("1:1:2");
      assert (rsValues instanceof ArrayStructure);
      assert rsValues.getRank() == 1;
      assert rsValues.getShape()[0] == 1;

      StructureData sdata = (StructureData) rsValues.getObject(rsValues.getIndex());
      Array gdata = sdata.getArray("time");
      assert gdata instanceof ArrayInt.D0 : gdata.getClass().getName();
      ArrayInt.D0 tdata = (ArrayInt.D0) gdata;
      int t = tdata.get();
      assert t == 18;

      int t2 = sdata.getScalarInt("time");
      assert t2 == 18;

      /* Read the times: unlimited dimension */
      Variable time = rs.findVariable("time");
      Array timeValues = time.read();
      assert (timeValues instanceof ArrayInt.D0);
      ArrayInt.D0 ta = (ArrayInt.D0) timeValues;
      assert (ta.get() == 6) : ta.get();
    }
  }
}
