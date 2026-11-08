
import json
import math
import random
from pathlib import Path
from predict import predict_building

# --------------------------------------------------
# PATHS
# --------------------------------------------------

ROOT = Path(__file__).resolve().parents[2]
BUILDINGS_FILE = ROOT / "eqs" / "buildings.json"
RESULTS_FILE = ROOT / "eqs" / "earthquake_results.json"


# --------------------------------------------------
# EARTHQUAKE GENERATION
# --------------------------------------------------

METRES_PER_PIXEL = 5.0

def generate_earthquake(city_width: int, city_height: int) -> dict:
    """
    Generate a random earthquake event.

    Epicentre coordinates are measured in city pixels.
    """

    return {
        "epicentre_x": random.uniform(0, city_width),
        "epicentre_y": random.uniform(0, city_height),
        "magnitude": round(random.uniform(5.5, 7.5), 1),
        "depth_km": random.uniform(5, 20)
    }


# --------------------------------------------------
# BUILDING GEOMETRY
# --------------------------------------------------

def calculate_building_centre(building: dict) -> tuple[float, float]:
    """Calculate the centre of a building in pixel coordinates."""

    centre_x = building["x"] + building["width"] / 2
    centre_y = building["y"] + building["height"] / 2

    return centre_x, centre_y


def calculate_distance(
    building_x: float,
    building_y: float,
    epicentre_x: float,
    epicentre_y: float
) -> float:
    """Calculate 2D Euclidean distance in pixels."""

    return math.sqrt(
        (building_x - epicentre_x) ** 2
        + (building_y - epicentre_y) ** 2
    )

def pixels_to_km(distance_pixels: float) -> float:
    """Convert map pixel distance into kilometres."""

    distance_metres = distance_pixels * METRES_PER_PIXEL
    return distance_metres / 1000


# --------------------------------------------------
# MAIN SIMULATION
# --------------------------------------------------

def load_buildings() -> dict:
    """Load building data exported by the Java application."""

    with open(BUILDINGS_FILE, "r", encoding="utf-8") as file:
        return json.load(file)

def classify_damage(probability):
    if probability < 0.20:
        return "No / negligible damage"
    elif probability < 0.40:
        return "Slight damage"
    elif probability < 0.60:
        return "Moderate damage"
    elif probability < 0.80:
        return "Severe damage"
    else:
        return "Near / complete destruction"

def run_simulation():

    buildings = load_buildings()

    if not buildings:
        raise ValueError("No buildings found in buildings.json")

    city_width = max(
        building["x"] + building["width"]
        for building in buildings.values()
    )

    city_height = max(
        building["y"] + building["height"]
        for building in buildings.values()
    )

    earthquake = generate_earthquake(city_width, city_height)
    vs30_values = generate_vs30(buildings)

    print("\n========== EARTHQUAKE SIMULATION ==========")
    print(f"Epicentre: ({earthquake['epicentre_x']:.2f}, "
          f"{earthquake['epicentre_y']:.2f})")
    print(f"Magnitude: {earthquake['magnitude']}")
    print(f"Depth: {earthquake['depth_km']:.2f} km")

    results = {}

    print("\n========== BUILDING PREDICTIONS ==========")

    for building_id, building in buildings.items():

        centre_x, centre_y = calculate_building_centre(building)

        distance = calculate_distance(
            centre_x,
            centre_y,
            earthquake["epicentre_x"],
            earthquake["epicentre_y"]
        )

        distance_km = pixels_to_km(distance)
        vs30 = vs30_values[building_id]

        pga = calculate_pga(
            earthquake["magnitude"],
            distance_km,
            vs30
        )

        model_input = {
            "pga_shakemap_g": pga,
            "total_height_m": building["height"] * 2.0,
            "min_wall_index_pct": 0.0004,
            "vs30_shakemap": vs30,
        }

        prediction = predict_building(model_input)

        probability = prediction["critical_damage_probability"]
        damage_description = classify_damage(probability)

        # Convert probability into a numeric visual level.
        damage_level = min(5, int(probability * 5) + 1)

        results[building_id] = {
            **building,
            "distance_km": round(distance_km, 4),
            "pga": round(pga, 4),
            "vs30": round(vs30, 2),
            "damage_probability": round(probability, 4),
            "damage_level": damage_level,
            "damage_description": damage_description,
            "predicted_class": prediction["predicted_class"]
        }

        print(
            f"Building {building_id}: "
            f"PGA={pga:.4f} g, "
            f"VS30={vs30:.2f} m/s, "
            f"Damage probability={probability:.2%}, "
            f"Level={damage_level} ({damage_description})"
        )

    output = {
        "earthquake": {
            "epicentre_x": round(earthquake["epicentre_x"], 2),
            "epicentre_y": round(earthquake["epicentre_y"], 2),
            "magnitude": earthquake["magnitude"],
            "depth_km": round(earthquake["depth_km"], 2)
        },
        "buildings": results
    }

    with open(RESULTS_FILE, "w", encoding="utf-8") as file:
        json.dump(output, file, indent=4)

    print(f"\nResults exported successfully to: {RESULTS_FILE}")


def generate_vs30(buildings: dict) -> dict:
    """
    Generate spatially correlated VS30 values
    for every building.
    """

    zones = [
        {"min_x": 0, "max_x": 12, "vs30": 300},
        {"min_x": 12, "max_x": 24, "vs30": 420},
        {"min_x": 24, "max_x": 1000, "vs30": 650}
    ]

    results = {}

    for building_id, building in buildings.items():

        centre_x, centre_y = calculate_building_centre(building)

        zone = next(
            zone for zone in zones
            if zone["min_x"] <= centre_x < zone["max_x"]
        )

        # Add small variation within each zone
        vs30 = random.uniform(
            zone["vs30"] - 30,
            zone["vs30"] + 30
        )

        results[building_id] = round(vs30, 2)

    return results


def calculate_pga(
    magnitude: float,
    distance_km: float,
    vs30: float
) -> float:
    """
    Approximate Boore-Atkinson 2008 PGA prediction.

    Returns PGA in units of g.

    Uses:
    - BA08 magnitude and distance scaling coefficients for PGA
    - BA08 linear and nonlinear site-response equations
    - A simplified reference-rock PGA estimate

    The reference-rock PGA is approximated using the final
    BA08 PGA coefficients. This is not an exact reproduction
    of the original BA08 pga4nl calibration.
    """

    import math

    # Validate inputs
    if not 5.0 <= magnitude <= 8.0:
        raise ValueError("BA08 magnitude must be between 5 and 8.")

    if distance_km < 0:
        raise ValueError("Distance cannot be negative.")

    if not 180 <= vs30 <= 1300:
        raise ValueError("VS30 must be between 180 and 1300 m/s.")

    # BA08 PGA coefficients
    c1 = -0.66050
    c2 = 0.11970
    c3 = -0.01151
    h = 1.35

    e1 = -0.53804
    e5 = 0.28805
    e6 = -0.10164
    mh = 6.75

    m_ref = 4.5
    r_ref = 1.0

    # Effective distance (Equation 4)
    r = math.sqrt(distance_km ** 2 + h ** 2)

    # Magnitude scaling (Equation 5)
    if magnitude <= mh:
        f_m = (
            e1
            + e5 * (magnitude - mh)
            + e6 * (magnitude - mh) ** 2
        )
    else:
        f_m = e1 + e5 * (magnitude - mh)

    # Distance scaling (Equation 3)
    f_d = (
        (c1 + c2 * (magnitude - m_ref))
        * math.log(r / r_ref)
        + c3 * (r - r_ref)
    )

    # Approximate reference-rock PGA (g).
    pga_rock = math.exp(f_m + f_d)

    # BA08 site-response coefficients for PGA
    b_lin = -0.360
    b1 = -0.640
    b2 = -0.140

    v1 = 180.0
    v2 = 300.0
    v_ref = 760.0

    a1 = 0.03
    a2 = 0.09
    pga_low = 0.06

    # Linear site amplification
    f_lin = b_lin * math.log(vs30 / v_ref)

    # Calculate nonlinear slope b_nl
    if vs30 <= v1:
        b_nl = b1

    elif vs30 <= v2:
        b_nl = (
            (b1 - b2)
            * math.log(vs30 / v2)
            / math.log(v1 / v2)
            + b2
        )

    elif vs30 < v_ref:
        b_nl = (
            b2
            * math.log(vs30 / v_ref)
            / math.log(v2 / v_ref)
        )

    else:
        b_nl = 0.0

    # Nonlinear site amplification
    if pga_rock <= a1:

        f_nl = b_nl * math.log(pga_low / 0.1)

    elif pga_rock <= a2:

        delta_x = math.log(a2 / a1)
        delta_y = b_nl * math.log(a2 / pga_low)

        c = (3 * delta_y - b_nl * delta_x) / delta_x ** 2
        d = -(2 * delta_y - b_nl * delta_x) / delta_x ** 3

        x = math.log(pga_rock / a1)

        f_nl = (
            b_nl * math.log(pga_low / 0.1)
            + c * x ** 2
            + d * x ** 3
        )

    else:

        f_nl = b_nl * math.log(pga_rock / 0.1)

    # Final PGA including site response
    pga = pga_rock * math.exp(f_lin + f_nl)

    return pga


if __name__ == "__main__":
    run_simulation()
