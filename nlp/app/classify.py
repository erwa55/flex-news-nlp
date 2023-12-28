from transformers import pipeline

classifier = pipeline("zero-shot-classification", model="MoritzLaurer/DeBERTa-v3-base-mnli-fever-anli")
sequence_to_classify = "Angela Merkel is a politician in Germany and leader of the CDU"
candidate_labels = ["politics", "economy", "entertainment", "environment"]
output = classifier(sequence_to_classify, candidate_labels, multi_label=False)

# Pair each label with its score and find the label with the highest score
highest_score_label = max(zip(output['labels'], output['scores']), key=lambda pair: pair[1])[0]

# Print the label with the highest score
print(highest_score_label)
