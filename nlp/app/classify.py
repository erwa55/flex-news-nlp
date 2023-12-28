from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from transformers import pipeline

app = FastAPI()

# Initialize the classifier
classifier = pipeline("zero-shot-classification", model="MoritzLaurer/DeBERTa-v3-base-mnli-fever-anli")

class ClassificationRequest(BaseModel):
    sequence_to_classify: str
    candidate_labels: list

@app.post("/classify")
async def classify(request: ClassificationRequest):
    try:
        # Run the classifier using the request content
        output = classifier(request.sequence_to_classify, request.candidate_labels, multi_label=False)
        # Find the label with the highest score
        highest_score_label = max(zip(output['labels'], output['scores']), key=lambda pair: pair[1])[0]
        return {"label": highest_score_label}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# Run the API with uvicorn
# uvicorn.run(app, host="0.0.0.0", port=8000)
