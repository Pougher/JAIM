
import h5py
from pathlib import Path

# Locate the JAIM repository root
PROJECT_ROOT = Path(__file__).resolve().parents[8]

# Locate the HDF file
file_path = PROJECT_ROOT / "res" / "gaziantep_shakemap.hdf"

print("Looking for HDF file:", file_path)

with h5py.File(file_path, "r") as f:

    print("=== FILE METADATA ===")

    for key, value in f.attrs.items():
        print(key, ":", value)

    print("\n=== INFO JSON ===")

    info = f["dictionaries/info.json"][()]

    if isinstance(info, bytes):
        info = info.decode("utf-8")

    print(info)

    print("\n=== PGA DATASET ATTRIBUTES ===")

    pga = f["arrays/imts/GREATER_OF_TWO_HORIZONTAL/PGA/mean"]

    for key, value in pga.attrs.items():
        print(key, ":", value)
