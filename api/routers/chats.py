from datetime import datetime
from typing import Annotated

from fastapi import APIRouter, Depends
from pydantic import PositiveInt

from db.queries import ORM
from dependecies.auth import get_current_active_user
from schemas.message_schema import CreateMessage, Message, MessageInDb
from schemas.user_schemas import User

chat_router = APIRouter(prefix="/chats", tags=["chats"])

@chat_router.post("/{chat_id}/messages")
async def send_message(chat_id: PositiveInt, message: CreateMessage, current_user: User = Depends(get_current_active_user)):
    timestamp = datetime.now()
    receiver_id = await ORM.get_chat_member(chat_id, current_user.user_id)
    model = Message(**message.model_dump(), sender_id=current_user.user_id,receiver_id=receiver_id,
                    timestamp=timestamp, chat_id=chat_id)
    result = await ORM.send_message(model)
    return result

@chat_router.get("/{chat_id}")
async def get_chat(chat_id: PositiveInt, current_user: Annotated[User, Depends(get_current_active_user)]) -> list[MessageInDb]:
    messages = await ORM.get_chat(current_user.user_id, chat_id)
    message_array = []
    for message in messages:
        message_array.append(MessageInDb.model_validate(message))
    return message_array

@chat_router.patch("/{chat_id}/mark-read")
async def mark_read(current_user: Annotated[User, Depends(get_current_active_user)], chat_id: PositiveInt):
    response = await ORM.mark_read(current_user.user_id, chat_id)
    return response

@chat_router.post("/clear")
async def clear_chats(user: Annotated[User, Depends(get_current_active_user)]) :
    await ORM.create_tables()
    return {"detail": "Messages cleared"}


@chat_router.post("/")
async def create_direct_chat(user: Annotated[User, Depends(get_current_active_user)], user_id: PositiveInt):
    new_chat = await ORM.create_direct_chat(user.user_id, user_id)
    return new_chat
