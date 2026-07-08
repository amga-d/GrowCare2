"""
export_tflite_fp16.py
=====================
Exports the trained YOLO11n-cls disease classification model to TFLite FP16
format for Android deployment via the GrowCare app.

Conversion pipeline:
    best.pt  ──[Ultralytics]──►  best.onnx  ──[TF/ONNX]──►  disease_detection.tflite

Usage (from the GrowCare project root):
    /home/amgad/anaconda3/envs/vene/bin/python ML/export_tflite_fp16.py

    # Force re-export of ONNX (e.g. after updating best.pt):
    /home/amgad/anaconda3/envs/vene/bin/python ML/export_tflite_fp16.py --force-onnx

Output:
    ML/disease_detection.tflite         (~5 MB FP16)
    app/src/main/assets/disease_detection.tflite   (auto-copied)

Model info:
    Architecture : YOLO11n-cls (classification)
    Classes      : 38 (full PlantVillage)
    Input        : [1, 224, 224, 3]  NHWC  (normalized 0-1)
    Output       : [1, 38]           class probabilities

IMPORTANT: imgsz=224 is intentional. YOLO classification models run at
    224x224 internally. Setting imgsz=640 creates a [1, 640, 640, 3] TFLite
    tensor but the model still internally sub-samples to 224x224, producing
    garbage predictions on Android due to the mismatched input layout.
"""

import argparse
import shutil
from pathlib import Path
from ultralytics import YOLO

# ── Paths ─────────────────────────────────────────────────────────────────────
PROJECT_ROOT = Path(__file__).parent.parent          # GrowCare/
ML_DIR       = PROJECT_ROOT / "ML"
WEIGHTS_PT   = ML_DIR / "best.pt"
WEIGHTS_ONNX = ML_DIR / "best.onnx"
ASSETS_DIR   = PROJECT_ROOT / "app" / "src" / "main" / "assets"
OUTPUT_NAME  = "disease_detection.tflite"
# ──────────────────────────────────────────────────────────────────────────────


def export_onnx(model: YOLO) -> Path:
    """Step 1 — PT → ONNX (FP16, simplified).
    
    Re-run with --force-onnx whenever you update best.pt.
    Otherwise this step is skipped and the existing ONNX is reused.
    """
    print("\n── Step 1: PT → ONNX ───────────────────────────────────────────────")
    onnx_path = model.export(
        format="onnx",
        imgsz=224,       # YOLO-cls internal inference resolution
        half=True,       # FP16 weights
        simplify=True,   # onnxslim graph optimisation pass
        device="cpu",
        opset=20,        # highest opset supported by onnxslim / onnx2tf
    )
    exported = Path(onnx_path)
    print(f"✅ ONNX saved: {exported}  ({exported.stat().st_size / 1e6:.1f} MB)")
    return exported


def export_tflite(model: YOLO) -> Path:
    """Step 2 — ONNX → SavedModel → TFLite FP16.
    
    Ultralytics handles the full chain internally.
    The existing best.onnx is reused automatically if present.
    """
    print("\n── Step 2: ONNX → TFLite FP16 ─────────────────────────────────────")
    tflite_path = model.export(
        format="tflite",
        imgsz=224,      # YOLO-cls internal inference resolution — NOT 640!
        half=True,      # FP16 quantization (~5 MB vs ~10 MB FP32)
        int8=False,     # INT8 needs calibration data; FP16 needs none
        device="cpu",
        simplify=True,
    )
    exported = Path(tflite_path)
    print(f"✅ TFLite saved: {exported}  ({exported.stat().st_size / 1e6:.1f} MB)")
    return exported


def copy_to_assets(tflite_file: Path) -> Path:
    """Copy the exported TFLite model into the Android assets folder."""
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    target = ASSETS_DIR / OUTPUT_NAME
    shutil.copy2(tflite_file, target)
    print(f"✅ Copied to Android assets: {target}")
    return target


def verify_tensors(tflite_file: Path):
    """Print input/output tensor shapes for verification."""
    try:
        import tensorflow as tf
        interp = tf.lite.Interpreter(model_path=str(tflite_file))
        interp.allocate_tensors()
        print("\n── Tensor info ──────────────────────────────────────────────────")
        for label, fn in [("Input", interp.get_input_details),
                           ("Output", interp.get_output_details)]:
            for t in fn():
                print(f"  [{label}] shape={list(t['shape'])}  dtype={t['dtype'].__name__}  "
                      f"name={t['name']}")
        print("─────────────────────────────────────────────────────────────────")
    except Exception as e:
        print(f"⚠️  Could not verify tensors: {e}")


def main():
    parser = argparse.ArgumentParser(description="Export YOLO11n → TFLite FP16")
    parser.add_argument(
        "--force-onnx", action="store_true",
        help="Re-export ONNX from best.pt even if best.onnx already exists. "
             "Use this whenever you update best.pt with a newly trained model."
    )
    args = parser.parse_args()

    assert WEIGHTS_PT.exists(), f"Checkpoint not found: {WEIGHTS_PT}"
    print(f"Loading checkpoint: {WEIGHTS_PT}")
    model = YOLO(str(WEIGHTS_PT))

    # ── Step 1: PT → ONNX ────────────────────────────────────────────────────
    if args.force_onnx or not WEIGHTS_ONNX.exists():
        if args.force_onnx:
            print("--force-onnx: re-exporting ONNX from best.pt ...")
        else:
            print(f"No ONNX found at {WEIGHTS_ONNX}, exporting now ...")
        export_onnx(model)
    else:
        print(f"\n── Step 1: PT → ONNX (skipped — reusing {WEIGHTS_ONNX.name}) ─")
        print(f"   Run with --force-onnx to re-export after updating best.pt")

    # ── Step 2: ONNX → TFLite FP16 ───────────────────────────────────────────
    tflite_file = export_tflite(model)

    # ── Copy to Android assets ────────────────────────────────────────────────
    asset = copy_to_assets(tflite_file)

    # ── Verify tensor shapes ──────────────────────────────────────────────────
    verify_tensors(asset)

    print("\nDone! Rebuild the Android app to pick up the new model.\n")


if __name__ == "__main__":
    main()
