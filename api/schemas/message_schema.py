from pydantic import BaseModel, PositiveInt, ConfigDict
from datetime import datetime



class CreateMessage(BaseModel):
    message: str = ""
    file_id: PositiveInt | None = None

class Message(BaseModel):
    sender_id: PositiveInt
    receiver_id: PositiveInt
    file_id: PositiveInt | None = None
    chat_id: PositiveInt
    message: str
    timestamp: datetime
    read: bool = False
    read_at: datetime | None = None
    deleted: bool = False
    deleted_by: PositiveInt | None = None
    edited: bool = False
    edited_at: datetime | None = None

    model_config = ConfigDict(from_attributes=True)

class MessageInDb(Message):
    message_id: PositiveInt

    model_config = ConfigDict(from_attributes=True)
