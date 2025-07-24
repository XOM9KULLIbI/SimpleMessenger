from datetime import datetime
from typing import Annotated
import io
import uvicorn
from fastapi import FastAPI, HTTPException, Depends, status, File, UploadFile
from fastapi.security import OAuth2PasswordRequestForm
from pydantic import PositiveInt
from fastapi.responses import StreamingResponse

from api.dependecies.auth import get_password_hashed, verify_password, create_access_token, create_refresh_token, \
    verify_refresh_token, credentials_exception, get_current_active_user
from api.schemas.file_schema import FileInDb, FileMeta, MAX_FILE_SIZE
from api.schemas.message_schema import Message, MessageInDb, CreateMessage
from api.db.queries import ORM
from api.schemas.token import Token, TokenData
from api.schemas.user_schemas import UserCreate, User
import logging
logging.getLogger("multipart").setLevel(logging.WARNING)

app = FastAPI()

@app.get("/")
def root():
    return {"detail": "SimpleMessanger API"}

@app.post("/send")
async def send_message(message: CreateMessage, user: User = Depends(get_current_active_user)):
    timestamp = datetime.now()
    model = Message(**message.model_dump(), sender_id=user.user_id, timestamp=timestamp)
    result = await ORM.send_message(model)
    return result

@app.get("/dialog")
async def get_dialog(user: Annotated[User, Depends(get_current_active_user)], receiver_id: PositiveInt) -> list[MessageInDb]:
    messages = await ORM.get_dialog(user.user_id, receiver_id)
    message_array = []
    for message in messages:
        message_array.append(MessageInDb.model_validate(message))
    return message_array

@app.patch("/mark-read")
async def mark_read(user: Annotated[User, Depends(get_current_active_user)], receiver_id: PositiveInt):
    response = await ORM.mark_read(user.user_id, receiver_id)
    return response

@app.post("/clear")
async def clear_messages(user: Annotated[User, Depends(get_current_active_user)]) :
    await ORM.create_tables()
    return {"detail": "Messages cleared"}

@app.post("/register")
async def register_user(user: UserCreate):
    existing_user = await ORM.get_user_by_username(user.username)
    if existing_user:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Username already registered")
    hashed_password = get_password_hashed(user.password)
    user_data = user.model_dump()
    user_data["hashed_password"] = hashed_password
    user_data["is_disabled"] = False
    del user_data["password"]
    created_user = await ORM.register_user(user_data)
    return created_user

@app.post("/token")
async def login_for_access_token(form_data: OAuth2PasswordRequestForm = Depends()):
    user = await ORM.get_user_by_username(form_data.username)
    if not user or not verify_password(form_data.password, user["hashed_password"]):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Incorrect username or password",
                            headers={"WWW-Authenticate": "Bearer"})
    access_token = create_access_token(data={"username": user["username"]})
    refresh_token = create_refresh_token(data={"username": user["username"]})
    return Token(access_token=access_token, token_type="bearer", refresh_token=refresh_token)

@app.post("/token/refresh", response_model=Token)
async def refresh_access_token(refresh_token: TokenData):
    username = verify_refresh_token(refresh_token.token)
    user = await ORM.get_user_by_username(username)
    if user is None:
        raise credentials_exception

    new_access_token = create_access_token(
        data={"username": user["username"]})

    return Token(token_type="bearer", access_token=new_access_token)

@app.post("/upload-file")
async def upload_file(file: Annotated[UploadFile, File()], user: Annotated[User, Depends(get_current_active_user)]):
    saved_file = await ORM.upload_file(file, user.user_id)
    return FileMeta.model_validate(saved_file)

@app.get("/files/{file_id}")
async def download_file(file_id: PositiveInt):
    file = await ORM.get_file_by_id(file_id)
    return StreamingResponse(io.BytesIO(file.data), media_type=file.content_type,
                             headers={"Content-Disposition": f"attachment; filename={file.filename}"})



if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)










