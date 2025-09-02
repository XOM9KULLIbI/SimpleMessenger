
import uvicorn
from fastapi import FastAPI
from starlette.middleware.cors import CORSMiddleware

from routers.main import router
app = FastAPI(
    title="SimpleMessenger API",
    description="API для мессенджера",
    version="1.0.0"
)

# app.add_middleware(
#     CORSMiddleware,
#     allow_origins=["*"],
#     allow_credentials=True,
#     allow_methods=["*"],
#     allow_headers=["*"],
# )

app.include_router(router, prefix="/api")



if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)










