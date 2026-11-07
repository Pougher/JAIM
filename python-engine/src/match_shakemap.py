
from pathlib import Path

import h5py
import numpy as np
import pandas as pd


# File paths

REPO_ROOT = Path(__file__).resolve().parents[2]

INPUT_CSV = (
    REPO_ROOT / "python-engine" / "data" / "processed"
    / "survey_buildings.csv"
)

INPUT_HDF = REPO_ROOT / "res" / "gaziantep_shakemap.hdf"

OUTPUT_CSV = (
    REPO_ROOT / "python-engine" / "data" / "processed"
    / "buildings_with_shakemap.csv"
)

# Bilinear interpolation

def interpolate_grid(grid, lat, lon, xmin, ymin, dx, dy):
    """
    Estimate a value at a coordinate using the four
    surrounding points in a regular grid.

    The grid's first row corresponds to ymax, so rows
    run from north to south.
    """

    ny, nx = grid.shape

    # Convert geographic coordinates into grid positions.
    col = (lon - xmin) / dx
    row = (ymin + (ny - 1) * dy - lat) / dy

    # Return NaN if the coordinate is outside the grid.
    if (
        col < 0 or col > nx - 1
        or row < 0 or row > ny - 1
    ):
        return np.nan

    # Identify the four surrounding grid cells.
    x0 = int(np.floor(col))
    y0 = int(np.floor(row))

    x1 = min(x0 + 1, nx - 1)
    y1 = min(y0 + 1, ny - 1)

    # Fractional distances between grid cells.
    fx = col - x0
    fy = row - y0

    # Values at the four corners.
    q11 = grid[y0, x0]
    q21 = grid[y0, x1]
    q12 = grid[y1, x0]
    q22 = grid[y1, x1]

    corners = np.array([q11, q21, q12, q22])

    if not np.all(np.isfinite(corners)):
        return np.nan

    # Bilinear interpolation.
    top = q11 * (1 - fx) + q21 * fx
    bottom = q12 * (1 - fx) + q22 * fx

    return top * (1 - fy) + bottom * fy


# 3. Main processing

def main():

    print("Loading building survey...")
    buildings = pd.read_csv(INPUT_CSV)

    print("Loading ShakeMap...")

    with h5py.File(INPUT_HDF, "r") as f:

        # PGA is stored as natural logarithm of g.
        pga_dataset = f[
            "arrays/imts/GREATER_OF_TWO_HORIZONTAL/PGA/mean"
        ]

        pga_ln = pga_dataset[:]
        pga_g = np.exp(pga_ln)

        # Read grid metadata.
        xmin = float(pga_dataset.attrs["xmin"])
        xmax = float(pga_dataset.attrs["xmax"])
        ymin = float(pga_dataset.attrs["ymin"])
        ymax = float(pga_dataset.attrs["ymax"])

        nx = int(pga_dataset.attrs["nx"])
        ny = int(pga_dataset.attrs["ny"])

        dx = (xmax - xmin) / (nx - 1)
        dy = (ymax - ymin) / (ny - 1)

        # Additional ShakeMap layers.
        mmi = f[
            "arrays/imts/GREATER_OF_TWO_HORIZONTAL/MMI/mean"
        ][:]

        vs30 = f["arrays/vs30"][:]

    # Match each building to the grid

    print("Matching buildings to ShakeMap...")

    buildings["pga_shakemap_g"] = buildings.apply(
        lambda building: interpolate_grid(
            pga_g,
            building["latitude"],
            building["longitude"],
            xmin, ymin, dx, dy
        ),
        axis=1
    )

    buildings["mmi_shakemap"] = buildings.apply(
        lambda building: interpolate_grid(
            mmi,
            building["latitude"],
            building["longitude"],
            xmin, ymin, dx, dy
        ),
        axis=1
    )

    buildings["vs30_shakemap"] = buildings.apply(
        lambda building: interpolate_grid(
            vs30,
            building["latitude"],
            building["longitude"],
            xmin, ymin, dx, dy
        ),
        axis=1
    )

    # Save and report

    buildings.to_csv(OUTPUT_CSV, index=False)

    matched = buildings["pga_shakemap_g"].notna().sum()
    unmatched = len(buildings) - matched

    print("\nMatching complete.")
    print(f"Total buildings: {len(buildings)}")
    print(f"Matched: {matched}")
    print(f"Outside grid or missing data: {unmatched}")

    print("\nInterpolated PGA statistics:")
    print(buildings["pga_shakemap_g"].describe())

    print("\nSaved to:")
    print(OUTPUT_CSV)


if __name__ == "__main__":
    main()
