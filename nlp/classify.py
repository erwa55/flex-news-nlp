from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from transformers import pipeline
from fastapi.responses import JSONResponse
import logging

app = FastAPI()

# Initialize logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize the classifier
classifier = pipeline("zero-shot-classification", model="MoritzLaurer/DeBERTa-v3-base-mnli-fever-anli")

class ClassificationRequest(BaseModel):
    sequence_to_classify: str
    candidate_labels: list

# Custom exception handler as middleware
@app.middleware("http")
async def exception_middleware(request: Request, call_next):
    try:
        return await call_next(request)
    except Exception as e:
        # Log the exception
        logger.exception(f"Unhandled error: {e}")
        # Return a generic response to the client
        return JSONResponse(
            status_code=500,
            content={"detail": "An internal server error occurred."}
        )

@app.post("/classify")
async def classify(request: ClassificationRequest):
    try:
        # Run the classifier using the request content
        output = classifier(request.sequence_to_classify, request.candidate_labels, multi_label=False)
        # Find the label with the highest score
        highest_score_label = max(zip(output['labels'], output['scores']), key=lambda pair: pair[1])[0]
        return {"label": highest_score_label}
    except Exception as e:
        # Log the specific exception for the endpoint
        logger.error(f"An error occurred during classification: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
