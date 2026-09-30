const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { setGlobalOptions } = require("firebase-functions/v2");
const logger = require("firebase-functions/logger");
const axios = require("axios");

// Set global options for cost control
setGlobalOptions({ maxInstances: 10 });

/**
 * Callable Cloud Function: fetchPlantImages
 * Triggered from Android using FirebaseFunctions.getInstance().getHttpsCallable("fetchPlantImages")
 */
exports.fetchPlantImages = onCall({ cors: true }, async (request) => {
  const plantName = request.data.plantName;

  if (!plantName) {
    throw new HttpsError(
      "invalid-argument",
      "The function must be called with a 'plantName' parameter."
    );
  }

  logger.info(`Fetching image data for: ${plantName}`);

  try {
    // Replace with your external image API query (e.g., Unsplash, Pixabay, or Perenual)
    // Example: Fetching image URLs from Unsplash API
    /*
    const response = await axios.get("https://api.unsplash.com/search/photos", {
      params: { query: plantName, per_page: 3 },
      headers: { Authorization: "Client-ID YOUR_UNSPLASH_ACCESS_KEY" }
    });
    const imageUrls = response.data.results.map((item) => item.urls.regular);
    */

    // Default response fallback
    const imageUrls = [
      `https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?auto=format&fit=crop&q=80`
    ];

    return {
      success: true,
      plantName: plantName,
      images: imageUrls,
    };
  } catch (error) {
    logger.error("Failed to fetch plant images:", error);
    throw new HttpsError("internal", "Error retrieving images for the requested plant.");
  }
});