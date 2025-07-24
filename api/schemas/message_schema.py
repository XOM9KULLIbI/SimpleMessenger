from pydantic import BaseModel, PositiveInt, ConfigDict
from datetime import datetime

from api.schemas.file_schema import FileInDb


class CreateMessage(BaseModel):
    receiver_id: PositiveInt
    message: str = ""
    file_id: PositiveInt | None = None

class Message(BaseModel):
    sender_id: PositiveInt
    receiver_id: PositiveInt
    file_id: PositiveInt | None = None
    message: str
    timestamp: datetime
    read: bool = False
    read_at: datetime | None = None
    deleted: bool = False

    model_config = ConfigDict(from_attributes=True)

class MessageInDb(Message):
    message_id: PositiveInt

    model_config = ConfigDict(from_attributes=True)
