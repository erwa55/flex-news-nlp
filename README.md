# Flex RSS ingest with Zero-Shot classification

I delved into a well-known yet persistently challenging problem: the organization and categorization of non-constant data sources. Utilizing  Artificial Intelligence (AI) and Dalet Flex to solve it.

To illustrate the challenge, let's look at automating the import and categorization of articles from various RSS feeds into Dalet Flex. Each article needs to be intelligently categorized based on its content, aligning with the predefined taxonomy in Dalet Flex. This task is not just about sorting information; it's about respecting and adapting to an established categorization system, ensuring that each article finds its rightful place.

## Why Zero-shot

First of all, we had to evaluate the right method for analyzing the articles. The solution needed to be both agile and robust, adaptable to on-premise or cloud infrastructures, and respectful of the established data models within Flex. 

Traditional NLP classifiers offer precision but lack flexibility, and while conversational Large Language Models (LLMs) like GPT or LLMA2 are powerful, they demand substantial resources.

After many tests and evaluations, Zero-shot classification (https://huggingface.co/tasks/zero-shot-classification) emerged as the hero of our story. This approach offered an blend of performance and adaptability, perfectly aligning with our needs. The chosen model was a variant of the pre-trained Microsoft DeBERTa-v3 model, available here: https://huggingface.co/MoritzLaurer/DeBERTa-v3-base-mnli-fever-anli

## Implementation

With the model selected, the next phase was the operationalization. The journey began with encapsulating the model into a docker image, making it a ready-to-deploy solution (docker image available on the repo).  Then an Flex workflow was configured to import articles from various RSS feeds, transforming each article into a User Defined Object (UDO), complete with all relevant metadata. 

For each article's description and existing taxonomy in Dalet Flex were fed into the Zero-shot classifier.  The classifier outputs the possible Flex category and  map it to the article, again using the Flex Scripting Framework. 

This entire process was automated and made efficient and scalable through the Flex Workflow Engine.

## How to 

Build the docker image present in the classifier folder


```bash


docker build -t zero-shot-classifier .

```


Run the docker image

```bash


docker run -p 8000:8000 zero-shot-classifier

```

Now you can test it using curl


```bash


curl -X 'POST' \
  'http://172.0.0.1:8000/classify' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "sequence_to_classify": "I love playing the piano and performing on stage.",
  "candidate_labels": ["music", "sports", "cooking"]
}'

```


Finally import the workflow definition and the script inside the FLex folder.
