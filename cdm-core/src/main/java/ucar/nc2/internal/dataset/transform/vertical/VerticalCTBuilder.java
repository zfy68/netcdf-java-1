/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.transform.vertical;

import ucar.nc2.AttributeContainer;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.VerticalCT;

import java.util.Formatter;

/**
 * A Builder of VerticalCT objects,
 * LOOK Problem is we need a NetcdfFile to extract variable data from.
 * 1) NetcdfFile is being constructed when we need it.
 * 2) GribGridDataset doesnt have a NetcdfFile.
 * 
 * @deprecated use ucar.nc2.geoloc.vertical
 */
@Deprecated
public interface VerticalCTBuilder {
  /**
   * Make a vertical VerticalCT.
   *
   * @param ds the containing file
   * @param ctv the attributes from the coordinate transform variable.
   * @return CoordinateTransform
   */
  VerticalCT.Builder<?> makeVerticalCT(NetcdfFile ds, AttributeContainer ctv);

  /** Get the VerticalCT name. */
  String getTransformName();

  /*** Pass in a Formatter where error messages can be appended. */
  void setErrorBuffer(Formatter sb);
}