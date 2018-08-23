/*
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright 2018 Astraea, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     [http://www.apache.org/licenses/LICENSE-2.0]
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package astraea.spark.rasterframes.tiles

import astraea.spark.rasterframes.ref.RasterSource
import astraea.spark.rasterframes.{TestData, TestEnvironment}
import geotrellis.raster.Tile
import geotrellis.vector.Extent

/**
 *
 *
 * @since 8/22/18
 */
//noinspection TypeAnnotation
class DelayedReadTileSpec extends TestEnvironment with TestData {
  def sub(e: Extent) = {
    val c = e.center
    val w = e.width
    val h = e.height
    Extent(c.x, c.y, c.x + w * 0.01, c.y + h * 0.01)
  }

  trait Fixture {
    val src = RasterSource(remoteCOGSingleband)
    val ext = sub(src.extent)
    val tile = new DelayedReadTile(ext, src)
  }

  describe("RasterRef") {
    it("should be realizable") {
      // NB: Had to test manually as to whether network traffic was
      // occurring or not.
      new Fixture {
        assert(tile.cellType === src.cellType)
        assert(tile.cols.toDouble === src.cols * 0.01 +- 2.0)
        assert(tile.rows.toDouble === src.rows * 0.01 +- 2.0)
        assert(tile.statistics.map(_.dataCells) === Some(tile.cols * tile.rows))
      }
    }
    it("should be Dataset compatible") {
      import astraea.spark.rasterframes.encoders.StandardEncoders._
      import spark.implicits._
      new Fixture {
        val ds = Seq(tile: Tile).toDS()
        ds.show(false)
      }
    }
    it("should serialize") {
      new Fixture {
        import java.io._

        val buf = new java.io.ByteArrayOutputStream()
        val out = new ObjectOutputStream(buf)
        out.writeObject(tile)
        out.close()
        val data = buf.toByteArray
        val in = new ObjectInputStream(new ByteArrayInputStream(data))
        val recovered = in.readObject()
        assert(tile === recovered)
      }
    }
  }
}