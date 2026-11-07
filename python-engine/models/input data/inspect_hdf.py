
import h5py
from pathlib import Path

# Locate the JAIM repository root
PROJECT_ROOT = Path(__file__).resolve().parents[8]

# Locate the HDF file
file_path = PROJECT_ROOT / "res" / "gaziantep_shakemap.hdf"

print("Looking for HDF file:", file_path)

with h5py.File(file_path, "r") as f:

    def inspect(name, obj):
        if isinstance(obj, h5py.Dataset):
            print(f"DATASET: {name}")
            print(f"Shape: {obj.shape}")
            print(f"Type: {obj.dtype}")
        else:
            print(f"GROUP: {name}")

    f.visititems(inspect)
