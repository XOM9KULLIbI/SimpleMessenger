from fastapi import APIRouter
from api.routers import auth, chats, files, users

router = APIRouter()
router.include_router(auth.auth_router)
router.include_router(chats.chat_router)
router.include_router(files.files_router)
router.include_router(users.user_router)

@router.get("/")
def root():
    return {"detail": "SimpleMessanger API"}