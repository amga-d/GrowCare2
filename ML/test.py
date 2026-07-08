from ultralytics import YOLO
import json

model = YOLO("best.pt")

image_path = "test.jpg"

# Run classification
results = model(image_path)[0]

# Classification results are stored in results.probs, not results.boxes
probs = results.probs

top1_id = int(probs.top1)
top1_conf = float(probs.top1conf)

top5 = []

for cls_id, conf in zip(probs.top5, probs.top5conf):
    cls_id = int(cls_id)
    top5.append({
        "class_id": cls_id,
        "class_name": model.names[cls_id],
        "confidence": float(conf)
    })

output = {
    "image": image_path,
    "prediction": {
        "class_id": top1_id,
        "class_name": model.names[top1_id],
        "confidence": top1_conf
    },
    "top5": top5
}

with open("classification_result.json", "w") as f:
    json.dump(output, f, indent=4)

print(json.dumps(output, indent=4))
print("Saved to classification_result.json")